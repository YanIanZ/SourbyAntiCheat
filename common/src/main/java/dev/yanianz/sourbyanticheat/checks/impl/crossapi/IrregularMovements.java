package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
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
    private int knockbackExemptTicks;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double directionChangeMinSpeed = 0.3;
    private double directionChangeThreshold = 0.95;
    private static final double NETTY_RATE_THRESHOLD = 120.0;

    public IrregularMovements(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        directionChangeMinSpeed  = config.getDoubleElse(base + "direction-change-min-speed", 0.3);
        directionChangeThreshold = config.getDoubleElse(base + "direction-change-threshold", 0.95);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        if (player.packetStateData.lastPacketWasTeleport
                || player.inVehicle() || player.canFly || player.isGliding
                || player.wasTouchingWater || player.compensatedEntities.self.isDead
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        // Knockback / explosion velocity reverses movement direction legitimately —
        // keep an exemption window for a few ticks after velocity is applied.
        if (player.likelyKB != null || player.firstBreadKB != null) {
            knockbackExemptTicks = 5;
        } else if (knockbackExemptTicks > 0) {
            knockbackExemptTicks--;
        }

        double deltaY = Math.abs(player.y - player.lastY);
        boolean jumpingOrFalling = deltaY > 0.1;
        double deltaX = player.x - player.lastX;
        double deltaZ = player.z - player.lastZ;
        double speed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (speed < directionChangeMinSpeed || jumpingOrFalling || knockbackExemptTicks > 0) {
            buffer = Math.max(0, buffer - 1);
            lastDeltaX = deltaX;
            lastDeltaZ = deltaZ;
            lastSpeed = speed;
            reward();
            return;
        }

        double prevSpeed = lastSpeed > 0.001 ? lastSpeed : 0.001;
        double dot = (deltaX * lastDeltaX + deltaZ * lastDeltaZ) / (speed * prevSpeed);
        boolean directionReversed = dot < -directionChangeThreshold && speed > directionChangeMinSpeed;

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
