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
import com.github.retrooper.packetevents.protocol.player.GameMode;

/**
 * Detects horizontal speed hacks.
 * <p>
 * Re-architected onto Grim's prediction engine: instead of comparing raw packet
 * deltas against hardcoded speed caps (which false-flags on ice, soul-sand, slime
 * bounces, knockback, riptide and Speed-effect amplifiers), this consumes Grim's
 * fully-simulated predicted velocity. The simulation already accounts for every
 * legitimate speed source — including the Speed-effect amplifier — so a sustained
 * prediction offset (the 3D distance between actual and predicted movement) is
 * genuine uncatchable movement. Using the offset rather than a scalar speed
 * magnitude also catches directional hacks that move at the predicted speed but
 * in the wrong direction.
 */
@CheckData(name = "Speed", stableKey = "sac.movement.speed", description = "Detects horizontal speed hacks", setback = 10, decay = 0.01)
public class Speed extends Check implements PostPredictionCheck {

    private double buffer = 0;

    // Config-wired thresholds (defaults equal prior hardcoded values).
    // SEMANTIC CHANGE (prediction re-architecture): baseSpeed / sprintSpeed are NO
    // LONGER speed caps. They are a scrutiny floor — the minimum horizontal movement
    // worth checking. Movement below this floor is rewarded outright and never
    // evaluated; it cannot trigger a flag regardless of how it compares to the
    // prediction. The actual speed cap is now Grim's per-tick predicted velocity:
    // the flag decision is `offset > offsetThreshold` (offset = 3D distance between
    // actual and predicted movement). Lowering these values widens scrutiny; it
    // does not lower the cap.
    private double baseSpeed = 0.217;
    private double sprintSpeed = 0.281;
    // offsetThreshold is the prediction offset (the 3D distance between actual and
    // predicted movement) a tick may carry before contributing to the buffer. Same
    // semantics as CrossSpeed's offset threshold — defaulted to its proven 0.15.
    private double offsetThreshold = 0.15;
    private double bufferDecay = 0.01;
    private double flagThreshold = 1.0;

    public Speed(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.baseSpeed = config.getDoubleElse(base + "base-speed", 0.217);
        this.sprintSpeed = config.getDoubleElse(base + "sprint-speed", 0.281);
        this.offsetThreshold = config.getDoubleElse(base + "offset-threshold", 0.15);
        this.bufferDecay = config.getDoubleElse(base + "buffer-decay", 0.01);
        this.flagThreshold = config.getDoubleElse(base + "flag-threshold", 1.0);
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        // Only act on a movement Grim actually simulated.
        if (!predictionComplete.isChecked()) return;

        // Creative-flight / spectator and elytra gliding have legitimate high speed
        // the horizontal-cap intent does not target — leave them to dedicated checks.
        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR) return;
        if (player.isFlying || player.canFly || player.isGliding || player.inVehicle()) return;

        double actX = player.actualMovement.getX();
        double actZ = player.actualMovement.getZ();
        double actualH = Math.sqrt(actX * actX + actZ * actZ);

        // Scrutiny floor (NOT a speed cap): movement slower than a walk is skipped
        // entirely. The cap that decides a flag is the prediction comparison below.
        double scrutinyFloor = player.isSprinting ? sprintSpeed : baseSpeed;
        if (actualH < scrutinyFloor) {
            buffer = Math.max(0, buffer - bufferDecay);
            reward();
            return;
        }

        // The predicted velocity already incorporates ice, soul-sand, slime bounces,
        // knockback, riptide and the Speed-effect amplifier. The true prediction
        // offset — the 3D distance between actual and predicted movement — is what
        // physics cannot explain. Unlike a scalar speed-magnitude residual, the
        // offset also catches a hack moving at the predicted speed in the wrong
        // direction (which would leave the magnitude residual near zero).
        double offX = player.actualMovement.getX() - player.predictedVelocity.vector.getX();
        double offY = player.actualMovement.getY() - player.predictedVelocity.vector.getY();
        double offZ = player.actualMovement.getZ() - player.predictedVelocity.vector.getZ();
        double offset = Math.sqrt(offX * offX + offY * offY + offZ * offZ);
        if (offset <= offsetThreshold) {
            buffer = Math.max(0, buffer - bufferDecay);
            reward();
            return;
        }

        buffer += offset - offsetThreshold;
        if (buffer > flagThreshold) {
            flagAndAlert("actH=" + String.format("%.3f", actualH)
                    + " offset=" + String.format("%.3f", offset)
                    + " buffer=" + String.format("%.3f", buffer));
            return;
        }
        // Sub-threshold but non-flagging — still reward to avoid VL stagnation.
        reward();
    }
}
