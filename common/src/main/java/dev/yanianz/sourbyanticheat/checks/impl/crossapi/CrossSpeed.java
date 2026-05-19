package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossSpeed", configName = "crossspeed", decay = 0.05, setback = 25, stableKey = "cross.speed")
public class CrossSpeed extends Check implements PostPredictionCheck {

    private double buffer;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double velocityMultiplier = 2.0;
    private double groundFriction     = 0.6;
    private double airFriction        = 0.91;

    public CrossSpeed(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        velocityMultiplier = config.getDoubleElse(base + "velocity-multiplier", 2.0);
        groundFriction     = config.getDoubleElse(base + "ground-friction", 0.6);
        airFriction        = config.getDoubleElse(base + "air-friction", 0.91);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.inVehicle() || player.compensatedEntities.self.isDead
                || player.packetStateData.lastPacketWasTeleport) return;

        double actualX = player.crossValidationData.pePositionDeltaX;
        double actualZ = player.crossValidationData.pePositionDeltaZ;
        double actualH = Math.sqrt(actualX * actualX + actualZ * actualZ);

        if (actualH < 0.01) {
            buffer = Math.max(0, buffer - 0.05);
            reward();
            return;
        }

        double velX = player.clientVelocity.getX();
        double velZ = player.clientVelocity.getZ();
        double velH = Math.sqrt(velX * velX + velZ * velZ);

        double friction = player.crossValidationData.peOnGround ? groundFriction : airFriction;
        double maxExpectedH = velH * velocityMultiplier;

        double offset = player.crossValidationData.offsetFromPrediction;
        boolean velocityFlag = actualH > maxExpectedH && velH > 0.01;
        boolean predictionFlag = offset > 0.15;

        if (!velocityFlag && !predictionFlag) {
            buffer = Math.max(0, buffer - 0.05);
            reward();
            return;
        }

        int ping = player.getTransactionPing();
        double pingMultiplier = ping > 400 ? 0.5 : 1.0;

        // Both flags false is unreachable here — the !velocityFlag && !predictionFlag clean
        // path returns above — so at least one of the two branches always applies.
        if (velocityFlag && predictionFlag) {
            buffer += 1.5 * pingMultiplier;
        } else {
            buffer += 0.75 * pingMultiplier;
        }

        if (buffer > 4.0) {
            // The velocity/prediction heuristic is noisy on its own — clientVelocity and
            // pePositionDelta are not tick-synchronised. Only alert when Spartan
            // independently confirms Speed for this player; otherwise decay and reward
            // so the heuristic alone cannot false-ban.
            SpartanCrossCheck.CrossCheckResult spartanResult =
                SpartanCrossCheck.checkSpartan(player.uuid, "Speed");
            if (spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED) {
                flagAndAlert(String.format("act=%.3f vel=%.3f off=%.3f netty=%.1f/s spartan=%s",
                    actualH, velH, offset,
                    player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
                return;
            }
            buffer = Math.max(0, buffer - 0.5);
        }
        reward();
    }
}
