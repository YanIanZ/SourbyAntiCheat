// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.combat;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;

/**
 * Improved AntiVelocity detection using PacketEvents' entity velocity packets.
 * <p>
 * Tracks outgoing velocity packets from the server (entity_velocity for the player's
 * entity ID) and then monitors subsequent movement packets to verify that the player
 * actually moved in response to the velocity. If velocity was applied but the player
 * shows zero or reversed movement for multiple ticks, flags.
 */
@CheckData(name = "AntiVelocity", stableKey = "sac.combat.antivelocity", description = "Detects anti-knockback via velocity packet tracking", setback = 15, decay = 0.01)
public class AntiVelocity extends Check implements PacketCheck {

    private double pendingVelocityX = 0;
    private double pendingVelocityZ = 0;
    private boolean velocityPending = false;
    private int ticksSinceVelocity = 0;
    private int buffer = 0;

    // How many ticks to wait for the player to respond to velocity
    private static final int VELOCITY_RESPONSE_TICKS = 5;
    // Minimum velocity magnitude to track (ignore tiny knockbacks)
    private static final double MIN_VELOCITY = 0.1;

    public AntiVelocity(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_VELOCITY) return;

        WrapperPlayServerEntityVelocity velocity = new WrapperPlayServerEntityVelocity(event);
        // Only track velocity packets sent to this player (their own entity ID)
        if (velocity.getEntityId() != player.entityID) return;

        double vx = velocity.getVelocity().getX();
        double vz = velocity.getVelocity().getZ();
        double magnitude = Math.sqrt(vx * vx + vz * vz);

        if (magnitude > MIN_VELOCITY) {
            // Use latency util to set this when the player receives it
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                pendingVelocityX = vx;
                pendingVelocityZ = vz;
                velocityPending = true;
                ticksSinceVelocity = 0;
            });
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.inVehicle() || player.isFlying || player.canFly || player.isGliding) return;

        if (!velocityPending) return;

        ticksSinceVelocity++;

        if (ticksSinceVelocity > VELOCITY_RESPONSE_TICKS) {
            // Check if the player moved in the direction of the velocity
            double deltaX = player.x - player.lastX;
            double deltaZ = player.z - player.lastZ;
            double horizontalDelta = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

            // If velocity was significant but player barely moved horizontally
            double expectedMagnitude = Math.sqrt(pendingVelocityX * pendingVelocityX + pendingVelocityZ * pendingVelocityZ);
            double ratio = horizontalDelta / Math.max(expectedMagnitude, 0.001);

            if (ratio < 0.1 && horizontalDelta < 0.05) {
                buffer++;
                if (buffer > 3) {
                    flagAndAlert("ratio=" + String.format("%.3f", ratio) + " expected=" + String.format("%.3f", expectedMagnitude) + " buffer=" + buffer);
                }
            } else {
                buffer = Math.max(0, buffer - 1);
                if (buffer < 2) reward();
            }

            velocityPending = false;
        }
    }
}
