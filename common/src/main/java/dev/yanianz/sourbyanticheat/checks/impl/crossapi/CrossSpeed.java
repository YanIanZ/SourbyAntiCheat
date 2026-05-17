package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossSpeed", configName = "crossspeed", decay = 0.05, setback = 25, stableKey = "cross.speed")
public class CrossSpeed extends Check implements PostPredictionCheck {

    private double buffer;
    private static final double PREDICTION_THRESHOLD = 0.5;
    private static final double NETTY_RATE_THRESHOLD = 18.0;
    private static final double NETTY_DELAY_THRESHOLD = 40.0;

    public CrossSpeed(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.inVehicle() || player.compensatedEntities.self.isDead) return;

        double offset = player.crossValidationData.offsetFromPrediction;
        offset = Math.min(offset, 3.0);
        player.crossValidationData.offsetFromPrediction = offset;

        boolean predictionFlag = offset > PREDICTION_THRESHOLD;

        if (!predictionFlag) {
            buffer = Math.max(0, buffer - 0.05);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD
            || player.crossValidationData.nettyAvgDelayBetweenPacketsMs < NETTY_DELAY_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Speed");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (predictionFlag && (nettyConfirms || spartanConfirms)) {
            buffer += 1.0;
            if (buffer > 3.0) {
                String verbose = String.format("offset=%.3f netty=%.1f/s spartan=%s",
                    offset,
                    player.crossValidationData.nettyPacketRatePerSec,
                    spartanResult.type());
                flagAndAlert(verbose);
            }
        } else if (predictionFlag) {
            buffer += 0.5;
            if (buffer > 5.0) {
                flagAndAlert(String.format("offset=%.3f (no cross-confirm)", offset));
            }
        }

        reward();
    }
}
