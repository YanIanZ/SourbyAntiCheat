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
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;

/**
 * AutoTotem — flags re-equipping the off-hand slot (45, player inventory) faster
 * than a human could click, repeatedly. That is the auto-totem signature; there
 * is no legitimate reason to spam off-hand swaps. Alert-only (setback = -1),
 * conservative, profile/leniency/exemption-aware via {@link Check}.
 */
@CheckData(name = "AutoTotem", stableKey = "sac.combat.autototem",
        description = "Inhuman off-hand (totem) re-equip speed", setback = -1, decay = 0.02)
public class AutoTotemA extends Check implements PacketCheck {

    private static final int OFFHAND_SLOT = 45;

    private long lastNanos = 0;
    private int fast = 0;
    private long minDelayMs = 80;
    private int threshold = 4;

    public AutoTotemA(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String b = getConfigName() + ".";
        minDelayMs = config.getIntElse(b + "min-delay", 80);
        threshold = config.getIntElse(b + "threshold", 4);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) return;

        WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);
        if (click.getWindowId() != 0 || click.getSlot() != OFFHAND_SLOT) return;

        long now = System.nanoTime();
        if (lastNanos > 0) {
            long ms = (now - lastNanos) / 1_000_000L;
            if (ms < minDelayMs) {
                fast++;
                if (fast > threshold) {
                    flagAndAlert("offhandSwap=" + fast + " delay=" + ms + "ms");
                }
            } else {
                fast = Math.max(0, fast - 2);
                if (fast < 2) reward();
            }
        }
        lastNanos = now;
    }
}
