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

    // Config-wired thresholds.
    // climb-ticks-threshold defaults to 10: a normal jump arc rises for only ~5-7 ticks
    // (even with Jump Boost) before gravity ends it, so a continuous-ascent streak above
    // this can only be an unbounded wall climb.
    private int climbTicksThreshold = 10;
    private int climbTickIncrement = 1;

    public Spider(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.climbTicksThreshold = config.getIntElse(base + "climb-ticks-threshold", 10);
        this.climbTickIncrement = config.getIntElse(base + "climb-tick-increment", 1);
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

        // A spider / wall-climb is CONTINUOUS upward movement while pressed against a
        // wall. A normal jump rises in open air (no horizontal collision) or, next to
        // a wall, rises for only a handful of ticks before gravity ends the arc.
        // Require BOTH wall contact and a strictly ascending tick; any non-ascending
        // tick or loss of wall contact resets the streak to zero, so a bounded jump
        // arc can never reach the threshold while an unbounded climb does.
        if (player.horizontalCollision && deltaY > 0.0) {
            climbTicks += climbTickIncrement;
            if (climbTicks > climbTicksThreshold) {
                flagAndAlert("dY=" + String.format("%.3f", deltaY) + " ticks=" + climbTicks);
                return;
            }
            reward();
        } else {
            climbTicks = 0;
            reward();
        }
    }
}
