package dev.yanianz.sourbyanticheat.checks.impl.scaffolding;

import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.BlockPlaceCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockPlace;

@CheckData(name = "ScaffoldB", stableKey = "sac.scaffolding.tower", description = "Detects tower scaffold (placing blocks below feet)", setback = 10, decay = 0.02)
public class ScaffoldB extends BlockPlaceCheck {

    private int towerCount = 0;
    private int lastY = Integer.MIN_VALUE;

    public ScaffoldB(SacPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        if (player.inVehicle() || player.isFlying || player.canFly) return;

        int blockY = place.position.getY();
        double playerY = player.y;
        int feetY = (int) Math.floor(playerY);

        if (blockY == feetY - 1 && player.isSneaking) {
            if (blockY == lastY) {
                towerCount++;
                if (towerCount > 4) {
                    flagAndAlert("tower=" + towerCount + " y=" + blockY);
                }
            } else {
                towerCount = 1;
            }
        } else {
            towerCount = Math.max(0, towerCount - 1);
            if (towerCount < 2) reward();
        }
        lastY = blockY;
    }
}
