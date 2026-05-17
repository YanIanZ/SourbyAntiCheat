package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossVehicle", configName = "crossvehicle", decay = 0.02, setback = 10, stableKey = "cross.vehicle")
public class CrossVehicle extends Check implements PostPredictionCheck {

    private double buffer;
    private static final double MAX_HORSE_SPEED = 0.45;
    private static final double MAX_BOAT_SPEED = 0.40;
    private static final double NETTY_RATE_THRESHOLD = 18.0;

    public CrossVehicle(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim || !player.inVehicle()) {
            reward();
            return;
        }

        double deltaX = player.x - player.lastX;
        double deltaZ = player.z - player.lastZ;
        double speed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        boolean speedFlag = speed > MAX_HORSE_SPEED;

        if (!speedFlag) {
            buffer = Math.max(0, buffer - 0.02);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "BoatMove");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 1.5 : 0.5;
        if (buffer > 3.0) {
            flagAndAlert(String.format("speed=%.2f netty=%.1f/s spartan=%s",
                speed, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
