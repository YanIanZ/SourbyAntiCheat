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

@CheckData(name = "AntiVelocity", stableKey = "sac.combat.antivelocity", description = "Detects anti-knockback via velocity packet tracking", setback = 15, decay = 0.01)
public class AntiVelocity extends Check implements PacketCheck {

    private double pendingVelocityX = 0;
    private double pendingVelocityZ = 0;
    private boolean velocityPending = false;
    private int ticksSinceVelocity = 0;
    private int buffer = 0;
    private double[] ratioSamples = new double[3];
    private int sampleIndex = 0;

    private static final int VELOCITY_RESPONSE_TICKS = 5;
    private static final double MIN_VELOCITY = 0.1;
    private static final double HORIZONTAL_FRICTION_GROUND = 0.91;
    private static final double HORIZONTAL_FRICTION_AIR = 0.98;

    public AntiVelocity(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_VELOCITY) return;

        WrapperPlayServerEntityVelocity velocity = new WrapperPlayServerEntityVelocity(event);
        if (velocity.getEntityId() != player.entityID) return;

        double vx = velocity.getVelocity().getX();
        double vz = velocity.getVelocity().getZ();
        double magnitude = Math.sqrt(vx * vx + vz * vz);

        if (magnitude > MIN_VELOCITY) {
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                pendingVelocityX = vx;
                pendingVelocityZ = vz;
                velocityPending = true;
                ticksSinceVelocity = 0;
                sampleIndex = 0;
            });
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.inVehicle() || player.isFlying || player.canFly || player.isGliding) return;

        // Reset on death/respawn
        if (player.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
            velocityPending = false;
            return;
        }

        if (!velocityPending) return;

        ticksSinceVelocity++;

        if (ticksSinceVelocity >= 2 && ticksSinceVelocity <= VELOCITY_RESPONSE_TICKS + 2) {
            double deltaX = player.x - player.lastX;
            double deltaZ = player.z - player.lastZ;
            double horizontalDelta = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

            double friction = player.onGround ? HORIZONTAL_FRICTION_GROUND : HORIZONTAL_FRICTION_AIR;
            double expectedAfterFriction = Math.sqrt(pendingVelocityX * pendingVelocityX + pendingVelocityZ * pendingVelocityZ) * friction;
            double ratio = horizontalDelta / Math.max(expectedAfterFriction, 0.001);

            if (sampleIndex < ratioSamples.length) {
                ratioSamples[sampleIndex++] = ratio;
            }
        }

        if (ticksSinceVelocity > VELOCITY_RESPONSE_TICKS + 2) {
            double avgRatio = 0;
            int validSamples = 0;
            for (int i = 0; i < sampleIndex; i++) {
                avgRatio += ratioSamples[i];
                validSamples++;
            }
            if (validSamples > 0) avgRatio /= validSamples;

            if (avgRatio < 0.1 && validSamples >= 2) {
                buffer++;
                if (buffer > 2) {
                    flagAndAlert("ratio=" + String.format("%.3f", avgRatio) + " samples=" + validSamples);
                }
            } else {
                buffer = Math.max(0, buffer - 1);
                if (buffer < 2) reward();
            }

            velocityPending = false;
        }
    }
}