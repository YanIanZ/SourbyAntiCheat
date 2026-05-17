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
    private static final double VELOCITY_MULTIPLIER = 2.0;
    private static final double GROUND_FRICTION = 0.6;
    private static final double AIR_FRICTION = 0.91;
    private static final double NETTY_RATE_THRESHOLD = 18.0;

    public CrossSpeed(SacPlayer player) {
        super(player);
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

        double friction = player.crossValidationData.peOnGround ? GROUND_FRICTION : AIR_FRICTION;
        double maxExpectedH = velH * VELOCITY_MULTIPLIER;

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

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;
        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Speed");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;
        boolean crossConfirm = nettyConfirms || spartanConfirms;

        if (velocityFlag && predictionFlag) {
            buffer += 1.5 * pingMultiplier;
        } else if (velocityFlag || predictionFlag) {
            buffer += 0.75 * pingMultiplier;
        } else {
            buffer += 0.3 * pingMultiplier;
        }

        if (buffer > 4.0) {
            flagAndAlert(String.format("act=%.3f vel=%.3f off=%.3f netty=%.1f/s spartan=%s",
                actualH, velH, offset,
                player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }

        reward();
    }
}
