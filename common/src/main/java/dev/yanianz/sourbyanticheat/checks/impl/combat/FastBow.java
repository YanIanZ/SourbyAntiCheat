// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.combat;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

/**
 * Improved FastBow detection using PacketEvents to track actual bow charge state.
 * <p>
 * A vanilla bow takes at least 3 ticks to charge (minimum ~150ms). This check tracks
 * the BLOCK_PLACEMENT packet (right-click to draw bow) and then detects if the release
 * packet comes too early. Also checks if the held item is actually a bow/crossbow.
 */
@CheckData(name = "FastBow", stableKey = "sac.combat.fastbow", description = "Detects rapid bow shooting via packet timing", setback = 5, decay = 0.02)
public class FastBow extends Check implements PacketCheck {

    private long bowDrawStart = 0;
    private boolean isDrawing = false;
    private int flags = 0;

    // Minimum charge time: 3 ticks = 150ms. Use 120ms for latency tolerance.
    private static final long MIN_CHARGE_TIME = 120;

    public FastBow(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Detect bow draw start (right-click with bow held)
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT
                || event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            var mainHand = player.inventory.getHeldItem();
            var offHand = player.inventory.getOffHand();

            boolean holdingBow = (mainHand.getType() == ItemTypes.BOW || mainHand.getType() == ItemTypes.CROSSBOW)
                    || (offHand.getType() == ItemTypes.BOW || offHand.getType() == ItemTypes.CROSSBOW);

            if (holdingBow && !isDrawing) {
                bowDrawStart = System.currentTimeMillis();
                isDrawing = true;
            }
        }

        // Detect bow release — PlayerDigging RELEASE_USE_ITEM
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            var digging = new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging(event);
            if (digging.getAction() == com.github.retrooper.packetevents.protocol.player.DiggingAction.RELEASE_USE_ITEM) {
                if (isDrawing) {
                    long chargeTime = System.currentTimeMillis() - bowDrawStart;
                    if (chargeTime < MIN_CHARGE_TIME && chargeTime > 0) {
                        flags++;
                        if (flags > 3) {
                            flagAndAlert("charge=" + chargeTime + "ms flags=" + flags);
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
}
