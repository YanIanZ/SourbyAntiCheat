// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.scaffolding;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.BlockPlaceCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockPlace;

/**
 * God-bridge signature: rapidly placing blocks at/below the feet while moving,
 * looking down, and NOT sneaking. Alert-only (setback = -1); distinct from
 * {@code ScaffoldA}'s interval/streak heuristic by the rotation + no-sneak combo.
 * Profile/leniency/exemption-aware via {@link dev.yanianz.sourbyanticheat.checks.Check}.
 */
@CheckData(name = "ScaffoldC", stableKey = "sac.scaffolding.godbridge",
        description = "God-bridge: fast downward placing while not sneaking", setback = -1, decay = 0.02)
public class ScaffoldC extends BlockPlaceCheck {

    private int streak = 0;
    private int streakThreshold = 6;
    private float minPitch = 40f;

    public ScaffoldC(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String b = getConfigName() + ".";
        streakThreshold = config.getIntElse(b + "streak-threshold", 6);
        minPitch = (float) config.getDoubleElse(b + "pitch", 40.0);
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        if (player.inVehicle() || player.isFlying || player.canFly) return;

        double dx = player.x - player.lastX;
        double dz = player.z - player.lastZ;
        boolean movingHoriz = Math.sqrt(dx * dx + dz * dz) > 0.15;
        boolean below = place.position.getY() < player.y;
        boolean lookDown = player.pitch > minPitch;

        if (below && movingHoriz && lookDown && !player.isSneaking) {
            streak++;
            if (streak > streakThreshold) {
                flagAndAlert("godbridge streak=" + streak + " pitch=" + String.format("%.0f", player.pitch));
            }
        } else {
            streak = Math.max(0, streak - 2);
            if (streak < 4) reward();
        }
    }
}
