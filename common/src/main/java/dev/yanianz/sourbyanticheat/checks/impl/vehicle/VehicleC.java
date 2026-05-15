package dev.yanianz.sourbyanticheat.checks.impl.vehicle;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.player.SacPlayer;

@CheckData(name = "VehicleC", stableKey = "sac.vehicle.vehicle_control")
public class VehicleC extends Check {
    public VehicleC(SacPlayer player) {
        super(player);
    }
}
