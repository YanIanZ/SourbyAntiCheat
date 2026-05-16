// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.movement;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

/**
 * Detects inventory walk/InventoryMove hacks — sending significant movement
 * while having a server-opened inventory (containers).
 * <p>
 * Vanilla clients freeze position while a container is open. Hacked clients
 * with InventoryMove send movement packets while interacting with containers.
 */
@CheckData(name = "InventoryMove", stableKey = "sac.movement.inventorymove", description = "Detects movement while inventory is open", setback = 10, decay = 0.025)
public class InventoryMove extends Check implements PacketCheck {

    private boolean hasOpenContainer = false;
    private int buffer = 0;

    public InventoryMove(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Track container open/close
        if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            hasOpenContainer = false;
            return;
        }

        // Server-opened inventories are tracked in SacPlayer
        if (player.serverOpenedInventoryThisTick) {
            hasOpenContainer = true;
        }

        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
        if (!flying.hasPositionChanged()) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.inVehicle() || player.isFlying || player.canFly) return;

        if (!hasOpenContainer) return;

        double deltaX = Math.abs(player.packetStateData.lastClaimedPosition.getX() - player.x);
        double deltaZ = Math.abs(player.packetStateData.lastClaimedPosition.getZ() - player.z);
        double horizontalDist = deltaX * deltaX + deltaZ * deltaZ;

        // Ignore negligible movement (0.03 threshold)
        if (horizontalDist < 0.005) return;

        // Player is moving with a container open
        buffer++;
        if (buffer > 3) {
            flagAndAlert("dist=" + String.format("%.4f", Math.sqrt(horizontalDist)) + " buffer=" + buffer);
        }
    }
}
