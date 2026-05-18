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
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;

@CheckData(name = "AntiVelocity", stableKey = "sac.combat.antivelocity", description = "Detects anti-knockback via velocity packet tracking", setback = 15, decay = 0.01)
public class AntiVelocity extends Check implements PacketCheck {

    private double pendingVelocityX = 0;
    private double pendingVelocityZ = 0;
    private boolean velocityPending = false;
    // Set by the latency lambda; consumed on the next receive to reset the window state.
    private boolean newVelocityArmed = false;
    private int ticksSinceVelocity = 0;
    private int buffer = 0;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private int velocityResponseTicks = 5;
    private double minVelocity = 0.1;
    private double groundFriction = 0.91;
    private double airFriction = 0.98;
    private double avgRatioThreshold = 0.1;
    private int bufferThreshold = 2;

    // Ring of ratio samples. The detection window spans ticksSinceVelocity 2..(responseTicks+2),
    // which yields up to (responseTicks + 1) samples — size the array to that real maximum.
    private double[] ratioSamples = new double[velocityResponseTicks + 1];
    private int sampleIndex = 0;

    public AntiVelocity(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.velocityResponseTicks = config.getIntElse(base + "velocity-response-ticks", 5);
        this.minVelocity = config.getDoubleElse(base + "min-velocity", 0.1);
        this.groundFriction = config.getDoubleElse(base + "ground-friction", 0.91);
        this.airFriction = config.getDoubleElse(base + "air-friction", 0.98);
        this.avgRatioThreshold = config.getDoubleElse(base + "avg-ratio-threshold", 0.1);
        this.bufferThreshold = config.getIntElse(base + "buffer-threshold", 2);
        this.ratioSamples = new double[velocityResponseTicks + 1];
        this.sampleIndex = 0;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_VELOCITY) return;

        WrapperPlayServerEntityVelocity velocity = new WrapperPlayServerEntityVelocity(event);
        if (velocity.getEntityId() != player.entityID) return;

        double vx = velocity.getVelocity().getX();
        double vz = velocity.getVelocity().getZ();
        double magnitude = Math.sqrt(vx * vx + vz * vz);

        if (magnitude > minVelocity) {
            // addRealTimeTask (non-async) runs this runnable synchronously on the same netty
            // packet thread when the matching transaction is processed — there is no thread
            // race here. The concern is ORDERING: the latency lambda fires interleaved with
            // the flying-packet window logic. So the lambda only records the pending velocity
            // and arms a flag; it does NOT reset window state (ticksSinceVelocity / sampleIndex)
            // directly. The actual window reset happens on the receive path, so the lambda can
            // never clobber an in-progress detection window.
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                pendingVelocityX = vx;
                pendingVelocityZ = vz;
                velocityPending = true;
                newVelocityArmed = true;
            });
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.inVehicle() || player.isFlying || player.canFly || player.isGliding) return;

        // Effects/states that legitimately alter knockback response — a player rowing a
        // boat, levitating, slow-falling or riptiding will not move the "expected" amount.
        if (player.riptideSpinAttackTicks > 0
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.LEVITATION)
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.SLOW_FALLING)) {
            velocityPending = false;
            return;
        }

        // Reset on death/respawn
        if (player.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
            velocityPending = false;
            return;
        }

        if (!velocityPending) return;

        // A freshly-armed velocity starts a new window: reset window state here on the
        // receive path (consistent with the sampleIndex reset below), never in the lambda.
        if (newVelocityArmed) {
            newVelocityArmed = false;
            ticksSinceVelocity = 0;
        }

        ticksSinceVelocity++;

        // Start of a fresh detection window — clear the sample buffer here, on the check
        // thread, so window state is never mutated from a deferred latency task.
        if (ticksSinceVelocity == 2) {
            sampleIndex = 0;
        }

        if (ticksSinceVelocity >= 2 && ticksSinceVelocity <= velocityResponseTicks + 2) {
            double deltaX = player.x - player.lastX;
            double deltaZ = player.z - player.lastZ;
            double horizontalDelta = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

            double friction = player.onGround ? groundFriction : airFriction;
            double expectedAfterFriction = Math.sqrt(pendingVelocityX * pendingVelocityX + pendingVelocityZ * pendingVelocityZ) * friction;
            double ratio = horizontalDelta / Math.max(expectedAfterFriction, 0.001);

            if (sampleIndex < ratioSamples.length) {
                ratioSamples[sampleIndex++] = ratio;
            }
        }

        if (ticksSinceVelocity > velocityResponseTicks + 2) {
            double avgRatio = 0;
            int validSamples = 0;
            for (int i = 0; i < sampleIndex; i++) {
                avgRatio += ratioSamples[i];
                validSamples++;
            }
            if (validSamples > 0) avgRatio /= validSamples;

            // A near-zero response ratio can also be a player simply pressed against a wall —
            // the collision absorbs the knockback legitimately. Require the player NOT to be
            // horizontally colliding before treating a low ratio as anti-knockback.
            boolean lowResponse = avgRatio < avgRatioThreshold && validSamples >= 2 && !player.horizontalCollision;

            if (lowResponse) {
                buffer++;
                if (buffer > bufferThreshold) {
                    flagAndAlert("ratio=" + String.format("%.3f", avgRatio) + " samples=" + validSamples);
                }
            } else {
                buffer = Math.max(0, buffer - 1);
                reward();
            }

            velocityPending = false;
        }
    }
}
