package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "IrregularMovements", configName = "irregularmovements", decay = 0.02, setback = 10, stableKey = "cross.irregularmovements")
public class IrregularMovements extends Check implements PostPredictionCheck {

    private int buffer;
    private double lastDeltaX, lastDeltaZ;
    private double lastSpeed;
    private static final double DIRECTION_CHANGE_MIN_SPEED = 0.3;
    private static final double DIRECTION_CHANGE_THRESHOLD = 0.95;
    private static final double NETTY_RATE_THRESHOLD = 20.0;

    public IrregularMovements(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        if (player.packetStateData.lastPacketWasTeleport
                || player.inVehicle() || player.canFly || player.isGliding
                || player.wasTouchingWater || player.compensatedEntities.self.isDead
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        double deltaY = Math.abs(player.y - player.lastY);
        boolean jumpingOrFalling = deltaY > 0.1;
        double deltaX = player.x - player.lastX;
        double deltaZ = player.z - player.lastZ;
        double speed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (speed < DIRECTION_CHANGE_MIN_SPEED || jumpingOrFalling) {
            buffer = Math.max(0, buffer - 1);
            lastDeltaX = deltaX;
            lastDeltaZ = deltaZ;
            lastSpeed = speed;
            reward();
            return;
        }

        double prevSpeed = lastSpeed > 0.001 ? lastSpeed : 0.001;
        double dot = (deltaX * lastDeltaX + deltaZ * lastDeltaZ) / (speed * prevSpeed);
        boolean directionReversed = dot < -DIRECTION_CHANGE_THRESHOLD && speed > DIRECTION_CHANGE_MIN_SPEED;

        lastDeltaX = deltaX;
        lastDeltaZ = deltaZ;
        lastSpeed = speed;

        if (!directionReversed) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "IrregularMovements");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            buffer += 2;
        } else {
            buffer += 1;
        }

        if (buffer > 4) {
            flagAndAlert(String.format("dot=%.3f speed=%.2f netty=%.1f/s spartan=%s",
                dot, speed,
                player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
