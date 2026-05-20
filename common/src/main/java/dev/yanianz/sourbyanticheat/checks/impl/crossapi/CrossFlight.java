package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import dev.yanianz.sourbyanticheat.utils.viaversion.ViaVersionUtil;

@CheckData(name = "CrossFlight", configName = "crossflight", decay = 0.05, setback = 25, stableKey = "cross.flight")
public class CrossFlight extends Check implements PostPredictionCheck {

    private double buffer;

    private double predictionThreshold = 0.15;
    private double nettyRateThreshold  = 120.0;
    private static final double CROSS_VERSION_LENIENCY = 1.5;
    private static final double VIA_BACKWARDS_Y_LENIENCY = 1.3;

    public CrossFlight(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.predictionThreshold = config.getDoubleElse(base + "prediction-threshold",  0.15);
        this.nettyRateThreshold  = config.getDoubleElse(base + "netty-rate-threshold",  120.0);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.compensatedEntities.self.hasPotionEffect(PotionTypes.LEVITATION)) return;
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.inVehicle() || player.compensatedEntities.self.isDead) return;

        // Slow-Falling causes slower-than-normal descent — exempt to avoid FP
        if (player.compensatedEntities.self.hasPotionEffect(PotionTypes.SLOW_FALLING)) {
            buffer = Math.max(0, buffer - 0.05);
            reward();
            return;
        }

        double offset = player.crossValidationData.offsetFromPrediction;
        double effectiveThreshold = predictionThreshold;
        if (ViaVersionUtil.isCrossVersion(player)) {
            effectiveThreshold *= CROSS_VERSION_LENIENCY;
        }
        if (ViaVersionUtil.isViaBackwardsPre1_9(player)) {
            effectiveThreshold *= VIA_BACKWARDS_Y_LENIENCY;
        }

        double deltaY = player.crossValidationData.pePositionDeltaY;
        double deltaYFloor = -0.01;
        if (ViaVersionUtil.isViaBackwardsPre1_9(player)) {
            deltaYFloor = -0.08;
        }
        boolean notFalling = deltaY > deltaYFloor;
        boolean predictionFlag = offset > effectiveThreshold && notFalling;

        if (!predictionFlag) {
            buffer = Math.max(0, buffer - 0.05);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > nettyRateThreshold;

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
                return;
            }
        } else {
            buffer += 0.5 * multiplier;
            if (buffer > 8.0) {
                flagAndAlert(String.format("offset=%.3f (no cross-confirm)", offset));
                return;
            }
        }
        // No reward() on suspicious ticks — reward() only on confirmed-clean paths above.
    }
}
