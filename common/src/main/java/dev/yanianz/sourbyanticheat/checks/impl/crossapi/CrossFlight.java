package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;

@CheckData(name = "CrossFlight", configName = "crossflight", decay = 0.05, setback = 25, stableKey = "cross.flight")
public class CrossFlight extends Check implements PostPredictionCheck {

    private double buffer;
    private static final double PREDICTION_THRESHOLD = 0.15;
    private static final double NETTY_RATE_THRESHOLD = 18.0;

    public CrossFlight(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.compensatedEntities.self.hasPotionEffect(PotionTypes.LEVITATION)) return;
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.inVehicle() || player.compensatedEntities.self.isDead) return;

        double offset = player.crossValidationData.offsetFromPrediction;
        boolean notFalling = player.crossValidationData.pePositionDeltaY >= 0;
        boolean predictionFlag = offset > PREDICTION_THRESHOLD && notFalling;

        if (!predictionFlag) {
            buffer = Math.max(0, buffer - 0.05);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Flight");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        int ping = player.getTransactionPing();
        double multiplier = ping > 400 ? 0.5 : 1.0;

        if (nettyConfirms || spartanConfirms) {
            buffer += 1.5 * multiplier;
            if (buffer > 5.0) {
                flagAndAlert(String.format("offset=%.3f netty=%.1f/s spartan=%s",
                    offset, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
            }
        } else {
            buffer += 0.5 * multiplier;
            if (buffer > 8.0) {
                flagAndAlert(String.format("offset=%.3f (no cross-confirm)", offset));
            }
        }

        reward();
    }
}
