// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.movement;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "Speed", stableKey = "sac.movement.speed", description = "Detects horizontal speed hacks", setback = 10, decay = 0.01)
public class Speed extends Check implements PostPredictionCheck {

    private static final double MAX_WALK_SPEED = 0.217;
    private static final double MAX_SPRINT_SPEED = 0.281;
    private static final double MAX_EFFECT_SPEED = 0.45;
    private static final double BUFFER_DECAY = 0.01;

    private double lastDeltaH = 0;
    private double buffer = 0;

    public Speed(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate) return;

        double deltaX = player.x - player.lastX;
        double deltaZ = player.z - player.lastZ;
        double deltaH = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double deltaY = Math.abs(player.y - player.lastY);

        if (deltaH < 0.001) {
            buffer = Math.max(0, buffer - BUFFER_DECAY);
            return;
        }

        double maxSpeed;
        if (player.isFlying || player.canFly) {
            maxSpeed = MAX_EFFECT_SPEED;
        } else if (player.isSprinting) {
            maxSpeed = MAX_SPRINT_SPEED;
        } else {
            maxSpeed = MAX_WALK_SPEED;
        }

        double excess = deltaH - maxSpeed;
        if (excess > 0.01) {
            buffer += excess;
            if (buffer > 1.0) {
                flagAndAlert("h=" + String.format("%.3f", deltaH) + " max=" + String.format("%.3f", maxSpeed) + " buffer=" + String.format("%.3f", buffer));
            }
        } else {
            buffer = Math.max(0, buffer - BUFFER_DECAY);
            reward();
        }

        lastDeltaH = deltaH;
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
    }
}
