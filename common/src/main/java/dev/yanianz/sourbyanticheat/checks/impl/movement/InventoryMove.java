// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.movement;

import ac.grim.grimac.api.config.ConfigManager;
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
    // Set when the server opens an inventory; promoted to hasOpenContainer on the NEXT flying
    // packet so the same-invocation set+check race cannot flag the first post-open tick.
    private boolean pendingOpen = false;
    private int buffer = 0;

    // move-threshold is compared against a SQUARED horizontal distance (deltaX^2 + deltaZ^2).
    // Default 0.0009 == (0.03 blocks)^2 — a small genuine wobble allowance. The prior code
    // compared 0.005 against the squared distance while the verbose printed sqrt, giving an
    // effective ~0.07-block threshold; this value restores the intended ~0.03-block gate.
    private double moveThresholdSq = 0.0009;

    public InventoryMove(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        this.moveThresholdSq = config.getDoubleElse(getConfigName() + ".move-threshold", 0.0009);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.CLOSE_WINDOW) {
            hasOpenContainer = false;
            pendingOpen = false;
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            hasOpenContainer = false;
            pendingOpen = false;
            return;
        }

        if (player.serverOpenedInventoryThisTick) {
            pendingOpen = true;
        }

        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
        if (!flying.hasPositionChanged()) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.inVehicle() || player.isFlying || player.canFly) return;

        // Promote a pending open one tick after it was registered.
        if (pendingOpen && !hasOpenContainer) {
            pendingOpen = false;
            hasOpenContainer = true;
            return;
        }

        if (!hasOpenContainer) {
            reward();
            return;
        }

        // Knockback/velocity exemption — server-pushed motion is not voluntary inventory walking.
        if (player.likelyKB != null || player.likelyExplosions != null
                || player.firstBreadKB != null || player.firstBreadExplosion != null) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        double deltaX = Math.abs(player.packetStateData.lastClaimedPosition.getX() - player.x);
        double deltaZ = Math.abs(player.packetStateData.lastClaimedPosition.getZ() - player.z);
        double horizontalDistSq = deltaX * deltaX + deltaZ * deltaZ;

        if (horizontalDistSq < moveThresholdSq) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        buffer++;
        if (buffer > 3) {
            flagAndAlert("distSq=" + String.format("%.4f", horizontalDistSq) + " buffer=" + buffer);
        }
    }
}
