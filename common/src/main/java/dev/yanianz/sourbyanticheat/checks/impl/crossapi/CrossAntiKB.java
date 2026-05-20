package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossAntiKB", configName = "crossantikb", decay = 0.15, setback = 15, stableKey = "cross.antikb")
public class CrossAntiKB extends Check implements PostPredictionCheck {

    private int buffer;

    private double ratioThreshold = 0.5;
    private double minPredictedMovement = 0.15;
    private static final int BUFFER_CAP = 5;

    public CrossAntiKB(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        ratioThreshold       = config.getDoubleElse(base + "ratio-threshold", 0.5);
        minPredictedMovement = config.getDoubleElse(base + "min-predicted-movement", 0.15);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.compensatedEntities.self.isDead) return;

        boolean hasVelocity = player.likelyKB != null || player.firstBreadKB != null;
        if (!hasVelocity) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        double actualMovement = Math.sqrt(
            player.crossValidationData.pePositionDeltaX * player.crossValidationData.pePositionDeltaX
            + player.crossValidationData.pePositionDeltaZ * player.crossValidationData.pePositionDeltaZ
        );
        double predictedMovement = Math.sqrt(
            player.crossValidationData.predictedDeltaX * player.crossValidationData.predictedDeltaX
            + player.crossValidationData.predictedDeltaZ * player.crossValidationData.predictedDeltaZ
        );

        if (predictedMovement < minPredictedMovement) {
            reward();
            return;
        }

        double ratio = actualMovement / predictedMovement;

        if (ratio < ratioThreshold) {
            SpartanCrossCheck.CrossCheckResult spartanResult =
                SpartanCrossCheck.checkSpartan(player.uuid, "AntiVelocity");
            boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

            buffer = Math.min(BUFFER_CAP, buffer + 1);

            if (spartanConfirms || buffer >= 2) {
                flagAndAlert(String.format("ratio=%.2f predicted=%.3f actual=%.3f vl=%d spartan=%s",
                    ratio, predictedMovement, actualMovement, (int) violations,
                    spartanResult.type()));
                buffer = 0;
            }
        } else {
            buffer = Math.max(0, buffer - 1);
            reward();
        }
    }
}
