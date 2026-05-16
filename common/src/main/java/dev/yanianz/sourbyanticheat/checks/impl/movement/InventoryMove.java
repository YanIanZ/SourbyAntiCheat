// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.movement;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "InventoryMove", stableKey = "sac.movement.inventorymove", description = "Detects movement while inventory is open", setback = 10, decay = 0.025)
public class InventoryMove extends Check implements PacketCheck {

    private boolean hasOpenContainer = false;
    private int buffer = 0;

    public InventoryMove(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.CLOSE_WINDOW) {
            hasOpenContainer = false;
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            hasOpenContainer = false;
            return;
        }

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

        if (horizontalDist < 0.005) return;

        buffer++;
        if (buffer > 3) {
            flagAndAlert("dist=" + String.format("%.4f", Math.sqrt(horizontalDist)) + " buffer=" + buffer);
        }
    }
}