// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.movement;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

/**
 * Blatant horizontal-speed net (cap-based). Alert-only (setback = -1) — subtle
 * speed is left to the prediction-based {@code Speed} check; this only flags
 * sustained horizontal motion far above a sprint-jump (peaks ~0.6 b/tick).
 * Profile/leniency/exemption-aware via {@link Check}.
 */
@CheckData(name = "SpeedB", stableKey = "sac.movement.speedb",
        description = "Blatant horizontal speed (cap-based)", setback = -1, decay = 0.02)
public class SpeedB extends Check implements PostPredictionCheck {

    private int buffer = 0;
    private double maxHorizontal = 0.9;
    private int flagThreshold = 4;
    private int minFlag = 2;

    public SpeedB(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String b = getConfigName() + ".";
        maxHorizontal = config.getDoubleElse(b + "max-horizontal", 0.9);
        flagThreshold = config.getIntElse(b + "flag-threshold", 4);
        minFlag = config.getIntElse(b + "min-flag", 2);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.inVehicle() || player.isFlying || player.canFly
                || player.isGliding || player.isSwimming) return;
        if (player.packetStateData.lastPacketWasTeleport) return;

        double dx = player.x - player.lastX;
        double dz = player.z - player.lastZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        if (horizontal > maxHorizontal) {
            buffer++;
            if (buffer > flagThreshold) {
                flagAndAlert(String.format("h=%.3f", horizontal));
            }
        } else {
            buffer = Math.max(0, buffer - 1);
            if (buffer < minFlag) reward();
        }
    }
}
