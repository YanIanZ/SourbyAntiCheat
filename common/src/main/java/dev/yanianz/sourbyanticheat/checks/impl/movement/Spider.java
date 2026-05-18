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
import dev.yanianz.sourbyanticheat.utils.nmsutil.Collisions;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

/**
 * Detects spider / wall-climb hacks — sustained upward movement while airborne
 * and against a wall without a legitimate climbable.
 * <p>
 * Re-architected onto Grim's prediction engine: the upward delta is read from the
 * fully-simulated actual movement, and Grim's own ladder/vine/scaffold detection
 * ({@code isClimbing}) plus water state exempt every legitimate climb source.
 */
@CheckData(name = "Spider", stableKey = "sac.movement.spider", description = "Detects spider/wall climb hacks", setback = 10, decay = 0.02)
public class Spider extends Check implements PostPredictionCheck {

    private int climbTicks = 0;
    private boolean wasOnGround = true;

    // Config-wired thresholds (defaults equal prior hardcoded values).
    private double climbOffset = 0.1;
    private int climbTicksThreshold = 4;
    private int bufferIncrement = 1;

    public Spider(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.climbOffset = config.getDoubleElse(base + "climb-offset", 0.1);
        this.climbTicksThreshold = config.getIntElse(base + "climb-ticks-threshold", 4);
        this.bufferIncrement = config.getIntElse(base + "buffer-increment", 1);
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked()) return;

        if (player.canFly || player.isFlying || player.isGliding || player.inVehicle()) {
            climbTicks = 0;
            wasOnGround = true;
            return;
        }

        // Ladders, vines and scaffolding (isClimbing), water currents and an active
        // riptide spin all legitimately produce sustained upward movement — exempt.
        // Slime/honey blocks bounce or cling against a wall — also exempt.
        boolean bounceExempt = Collisions.hasMaterial(player,
                player.boundingBox.copy().expand(0.1),
                data -> data.first().getType() == StateTypes.SLIME_BLOCK
                    || data.first().getType() == StateTypes.HONEY_BLOCK);
        if (player.isClimbing || player.wasTouchingWater || player.riptideSpinAttackTicks > 0
                || bounceExempt) {
            climbTicks = 0;
            wasOnGround = true;
            reward();
            return;
        }

        if (player.onGround) {
            climbTicks = 0;
            wasOnGround = true;
            reward();
            return;
        }

        double deltaY = player.actualMovement.getY();

        // Only count an airborne climb once the player has genuinely left the ground.
        // The first airborne tick (wasOnGround still true) is the legitimate jump
        // arc — skip it so climbTicks is not incremented one tick early.
        if (wasOnGround) {
            wasOnGround = false;
            reward();
            return;
        }

        if (deltaY > climbOffset) {
            climbTicks += bufferIncrement;
            if (climbTicks > climbTicksThreshold) {
                flagAndAlert("dY=" + String.format("%.3f", deltaY) + " ticks=" + climbTicks);
                return;
            }
            reward();
        } else {
            climbTicks = Math.max(0, climbTicks - 2);
            reward();
        }
    }
}
