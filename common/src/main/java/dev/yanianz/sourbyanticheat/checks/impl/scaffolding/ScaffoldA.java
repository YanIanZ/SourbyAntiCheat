package dev.yanianz.sourbyanticheat.checks.impl.scaffolding;

import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.BlockPlaceCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockPlace;

@CheckData(name = "ScaffoldA", stableKey = "sac.scaffolding.downward_bridge", description = "Detects scaffold bridging patterns", setback = 10, decay = 0.02)
public class ScaffoldA extends BlockPlaceCheck {

    private int placeStreak = 0;
    private double lastPlaceX, lastPlaceZ;

    public ScaffoldA(SacPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        if (player.inVehicle() || player.isFlying || player.canFly) return;

        double dx = Math.abs(place.position.getX() - lastPlaceX);
        double dz = Math.abs(place.position.getZ() - lastPlaceZ);
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist > 0.5 && dist < 6.0 && Math.abs(place.position.getY() - player.y) < 2.0) {
            placeStreak++;
            if (placeStreak > 15) {
                flagAndAlert("streak=" + placeStreak + " interval=" + String.format("%.1f", dist));
            }
        } else {
            placeStreak = Math.max(0, placeStreak - 2);
            if (placeStreak < 5) reward();
        }

        lastPlaceX = place.position.getX();
        lastPlaceZ = place.position.getZ();
    }
}
