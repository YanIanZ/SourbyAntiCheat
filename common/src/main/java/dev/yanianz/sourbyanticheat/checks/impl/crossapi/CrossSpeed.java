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

    public CrossSpeed(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        double offset = player.crossValidationData.offsetFromPrediction;
        offset = Math.min(offset, 3.0);
        player.crossValidationData.offsetFromPrediction = offset;

        boolean predictionFlag = offset > PREDICTION_THRESHOLD;

        if (!predictionFlag) {
            buffer = Math.max(0, buffer - 0.05);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > 22.0
            || player.crossValidationData.nettyAvgDelayBetweenPacketsMs < 45.0;

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
                flagAndAlertWithSetback(verbose);
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
