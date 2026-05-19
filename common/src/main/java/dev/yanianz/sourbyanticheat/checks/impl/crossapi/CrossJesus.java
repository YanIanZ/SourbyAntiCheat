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

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double offsetThreshold    = 0.15;
    private double nettyRateThreshold = 120.0;

    public CrossJesus(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        offsetThreshold    = config.getDoubleElse(base + "offset-threshold",     0.15);
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

        // Require player to be touching water but NOT submerged (eyes above surface).
        // wasEyeInWater = true means the player is swimming/submerged — normal swimming, not Jesus.
        // wasSwimming catches active swim-mode. Both cases are legitimate, skip them.
        boolean aboveWaterSurface = player.wasTouchingWater && !player.wasEyeInWater && !player.wasSwimming;
        double offset = player.crossValidationData.offsetFromPrediction;
        boolean predictionFlag = aboveWaterSurface && offset > offsetThreshold;

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

        if (nettyConfirms || spartanConfirms) {
            buffer += 1.5 * multiplier;
            if (buffer > 3.0) {
                flagAndAlert(String.format("offset=%.3f netty=%.1f/s spartan=%s",
                    offset, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
                return;
            }
        } else {
            buffer += 0.5 * multiplier;
            if (buffer > 5.0) {
                flagAndAlert(String.format("offset=%.3f (no cross-confirm)", offset));
                return;
            }
        }
        // reward() only on confirmed-clean paths above — not here (suspicious tick)
    }
}
