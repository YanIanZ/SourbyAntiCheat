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
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

/**
 * Improved FastBow detection using PacketEvents to track actual bow charge state.
 * <p>
 * A vanilla bow takes at least 3 ticks to charge (minimum ~150ms). This check tracks
 * the BLOCK_PLACEMENT packet (right-click to draw bow) and then detects if the release
 * packet comes too early. Also checks if the held item is actually a bow/crossbow.
 */
@CheckData(name = "FastBow", stableKey = "sac.combat.fastbow", description = "Detects rapid bow shooting via packet timing", setback = 5, decay = 0.02)
public class FastBow extends Check implements PacketCheck {

    // Monotonic charge-start timestamp (nanoTime) — immune to wall-clock/NTP jumps.
    private long bowDrawStartNanos = 0;
    private boolean isDrawing = false;
    private int flags = 0;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    // Minimum charge time: 3 ticks = 150ms. Use 120ms for latency tolerance.
    private long minChargeTime = 120;
    private int flagThreshold = 3;

    public FastBow(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.minChargeTime = config.getIntElse(base + "min-charge-time", 120);
        this.flagThreshold = config.getIntElse(base + "flag-threshold", 3);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Reset draw state on death and teleport — the in-progress draw is no longer valid.
        if (player.compensatedEntities.self.isDead
                || player.packetStateData.lastPacketWasTeleport) {
            isDrawing = false;
        }

        // Switching the held hotbar slot cancels an in-progress bow draw.
        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            isDrawing = false;
            return;
        }

        // Detect bow draw start (right-click with a bow actually in the main hand).
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT
                || event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            // Restrict to the main-hand bow only — a generic right-click or an offhand bow
            // must not register a false draw against a main-hand non-bow item.
            var mainHand = player.inventory.getHeldItem();
            boolean holdingBow = mainHand.getType() == ItemTypes.BOW
                    || mainHand.getType() == ItemTypes.CROSSBOW;

            if (holdingBow && !isDrawing) {
                bowDrawStartNanos = System.nanoTime();
                isDrawing = true;
            }
        }

        // Detect bow release — PlayerDigging RELEASE_USE_ITEM
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            var digging = new WrapperPlayClientPlayerDigging(event);
            if (digging.getAction() == DiggingAction.RELEASE_USE_ITEM && isDrawing) {
                long chargeTime = (System.nanoTime() - bowDrawStartNanos) / 1_000_000L;

                // Add player RTT as tolerance: a high-ping player's server-measured charge
                // appears shorter than the real client-side duration, so we grant the full
                // ping as benefit of the doubt (ADD, not subtract). Flag only when even the
                // ping-tolerant charge is still below minChargeTime.
                long ping = player.getTransactionPing();
                long tolerantCharge = chargeTime + ping;

                if (tolerantCharge < minChargeTime && chargeTime > 0) {
                    flags++;
                    if (flags > flagThreshold) {
                        flagAndAlert("charge=" + chargeTime + "ms tolerant=" + tolerantCharge
                                + "ms ping=" + ping + "ms flags=" + flags);
                    }
                } else {
                    flags = Math.max(0, flags - 1);
                    if (flags < 2) reward();
                }
                isDrawing = false;
            }
        }
    }
}
