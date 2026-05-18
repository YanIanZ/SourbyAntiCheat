// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow.WindowClickType;

/**
 * Detects automatic armor equipping — a module that re-equips armor pieces faster than
 * a human could click them. Triggers on clicks into the player-inventory armor slots
 * (slots 5-8 of window 0), not on window-close timing.
 */
@CheckData(name = "AutoArmor", stableKey = "sac.combat.autoarmor", description = "Detects automatic armor equipping", setback = 5, decay = 0.02)
public class AutoArmor extends Check implements PacketCheck {

    // Player-inventory armor slot range (helmet..boots) in window id 0.
    private static final int ARMOR_SLOT_MIN = 5;
    private static final int ARMOR_SLOT_MAX = 8;

    // Monotonic timestamp (nanoTime) of the previous armor-slot click.
    private long lastSwitchNanos = 0;
    private int fastSwitchCount = 0;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private long minSwitchDelayMs = 50;
    private int switchCountThreshold = 10;
    private int fastSwitchResetThreshold = 3;

    public AutoArmor(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.minSwitchDelayMs = config.getIntElse(base + "min-switch-delay", 50);
        this.switchCountThreshold = config.getIntElse(base + "switch-count-threshold", 10);
        this.fastSwitchResetThreshold = config.getIntElse(base + "fast-switch-threshold", 3);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) return;

        WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);

        // Only the player's own inventory (window 0). Chest/anvil/any open GUI is exempt —
        // moving armor around a container is normal.
        if (click.getWindowId() != 0) return;

        // Creative-mode inventory edits are unrestricted; exempt.
        if (player.gamemode == GameMode.CREATIVE) return;

        // Shift-click (quick-move) and hotbar-swap are legit one-action equips, not a
        // pick-up-then-place armor cycle — exempt.
        WindowClickType clickType = click.getWindowClickType();
        if (clickType == WindowClickType.QUICK_MOVE || clickType == WindowClickType.SWAP) return;

        // Only a direct click on an armor slot is an armor switch.
        int slot = click.getSlot();
        if (slot < ARMOR_SLOT_MIN || slot > ARMOR_SLOT_MAX) return;

        long now = System.nanoTime();
        if (lastSwitchNanos > 0) {
            long elapsedMs = (now - lastSwitchNanos) / 1_000_000L;
            if (elapsedMs < minSwitchDelayMs) {
                fastSwitchCount++;
                if (fastSwitchCount > switchCountThreshold) {
                    flagAndAlert("switch=" + fastSwitchCount + " delay=" + elapsedMs + "ms");
                    // Flag path — return before the clean-tick reward below.
                    lastSwitchNanos = now;
                    return;
                }
            } else {
                fastSwitchCount = Math.max(0, fastSwitchCount - 2);
                // Once the fast-switch streak has decayed below the reset threshold the
                // sequence is considered fully clean again.
                if (fastSwitchCount < fastSwitchResetThreshold) fastSwitchCount = 0;
            }
        }
        lastSwitchNanos = now;
        // Every non-flagging armor-interaction path (first click of a session, slow
        // switch, or a fast switch still under the count threshold) rewards exactly once.
        reward();
    }
}
