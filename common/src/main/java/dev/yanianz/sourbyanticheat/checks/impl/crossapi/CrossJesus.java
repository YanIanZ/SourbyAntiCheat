package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;

@CheckData(name = "CrossJesus", configName = "crossjesus", decay = 0.05, setback = 15, stableKey = "cross.jesus")
public class CrossJesus extends Check implements PostPredictionCheck {

    private double buffer;

    private double offsetThreshold    = 0.08;
    private double nettyRateThreshold = 120.0;
    private static final double BUFFER_CAP = 5.0;

    public CrossJesus(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        offsetThreshold    = config.getDoubleElse(base + "offset-threshold",     0.08);
        nettyRateThreshold = config.getDoubleElse(base + "netty-rate-threshold", 120.0);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.compensatedEntities.self.hasPotionEffect(PotionTypes.DOLPHINS_GRACE)) return;
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.inVehicle() || player.compensatedEntities.self.isDead) return;
        if (player.packetStateData.lastPacketWasTeleport) return;

        // Detect Jesus: player walks on water surface.
        // Must be touching water but NOT fully submerged and NOT actively swimming.
        // wasSwimming catches the 1.13+ swim-mode; wasEyeInWater catches head-submerged.
        // We also accept unknownWaterState = true (on 1.12- where eye-in-water is unreliable).
        boolean onWaterSurface = player.wasTouchingWater && !player.wasSwimming;
        if (!onWaterSurface) {
            buffer = Math.max(0, buffer - 0.05);
            reward();
            return;
        }

        double offset = player.crossValidationData.offsetFromPrediction;
        boolean predictionFlag = offset > offsetThreshold;

        if (!predictionFlag) {
            buffer = Math.max(0, buffer - 0.05);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > nettyRateThreshold;
        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Jesus");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        int ping = player.getTransactionPing();
        double multiplier = ping > 400 ? 0.5 : 1.0;

        if (spartanConfirms && nettyConfirms) {
            buffer = Math.min(BUFFER_CAP, buffer + 1.5 * multiplier);
            if (buffer > 3.0) {
                flagAndAlert(String.format("offset=%.3f netty=%.1f/s spartan=%s",
                    offset, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
                buffer = 0;
                return;
            }
        } else if (spartanConfirms || nettyConfirms) {
            buffer = Math.min(BUFFER_CAP, buffer + 0.8 * multiplier);
        } else {
            buffer = Math.max(0, buffer - 0.05);
        }
    }
}
