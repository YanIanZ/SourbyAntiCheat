package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossNoFall", configName = "crossnofall", decay = 0.15, setback = 15, stableKey = "cross.nofall")
public class CrossNoFall extends Check implements PostPredictionCheck {

    private double buffer;
    private int airborneTicks;
    private static final double OFFSET_THRESHOLD = 0.3;
    private static final double NETTY_DELAY_THRESHOLD = 50.0;

    public CrossNoFall(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        if (player.crossValidationData.peOnGround) {
            airborneTicks = 0;
        } else {
            airborneTicks++;
        }

        double yOffset = Math.abs(player.crossValidationData.pePositionDeltaY
            - player.crossValidationData.predictedDeltaY);
        boolean groundSpoof = player.crossValidationData.peOnGround
            && airborneTicks > 3
            && yOffset > OFFSET_THRESHOLD;

        if (!groundSpoof) {
            buffer = Math.max(0, buffer - 0.15);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyAvgDelayBetweenPacketsMs < NETTY_DELAY_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "NoFall");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            buffer += 1.5;
            if (buffer > 4.0) {
                flagAndAlertWithSetback(String.format("yOff=%.3f airTicks=%d netty=%.1fms spartan=%s",
                    yOffset, airborneTicks,
                    player.crossValidationData.nettyAvgDelayBetweenPacketsMs, spartanResult.type()));
            }
        } else {
            buffer += 0.5;
            if (buffer > 6.0) {
                flagAndAlert(String.format("yOff=%.3f airTicks=%d (no cross-confirm)",
                    yOffset, airborneTicks));
            }
        }

        reward();
    }
}
