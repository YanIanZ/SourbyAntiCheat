// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.RotationCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.RotationUpdate;

/**
 * Observe-only aim-acceleration check. Tracks the change in yaw-delta between
 * consecutive rotation updates; a sustained pattern of implausible acceleration
 * (large jerk on top of an already-large turn) is characteristic of some aim
 * assists. Alert-only (setback = -1) and intentionally lenient — it exists to
 * bank data, not to punish. Profile- and leniency-aware via {@link Check}.
 */
@CheckData(name = "AimAccel", stableKey = "sac.aim.accel",
        description = "Implausible rotation acceleration (observe-only)", setback = -1, decay = 0.05)
public class AimAccel extends Check implements RotationCheck {

    private float lastDeltaYaw = 0f;
    private int buffer = 0;
    private float minDelta = 8f;
    private float maxAccel = 35f;
    private int flagThreshold = 6;
    private int minFlag = 2;

    public AimAccel(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String b = getConfigName() + ".";
        minDelta = (float) config.getDoubleElse(b + "min-delta", 8.0);
        maxAccel = (float) config.getDoubleElse(b + "max-accel", 35.0);
        flagThreshold = config.getIntElse(b + "flag-threshold", 6);
        minFlag = config.getIntElse(b + "min-flag", 2);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        // A setback yaw is not a real player rotation — reset the baseline.
        if (player.packetStateData.lastPacketWasTeleport || player.inVehicle()) {
            lastDeltaYaw = rotationUpdate.getDeltaYRotABS();
            return;
        }

        float deltaYaw = rotationUpdate.getDeltaYRotABS();
        float accel = Math.abs(deltaYaw - lastDeltaYaw);

        if (deltaYaw > minDelta && accel > maxAccel) {
            buffer++;
            if (buffer > flagThreshold) {
                flagAndAlert(String.format("dYaw=%.1f accel=%.1f", deltaYaw, accel));
            }
        } else {
            buffer = Math.max(0, buffer - 1);
            if (buffer < minFlag) reward();
        }

        lastDeltaYaw = deltaYaw;
    }
}
