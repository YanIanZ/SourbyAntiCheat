package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;

@CheckData(name = "CrossNoFall", configName = "crossnofall", decay = 0.15, setback = 15, stableKey = "cross.nofall")
public class CrossNoFall extends Check implements PostPredictionCheck {

    private double buffer;

    private double offsetThreshold = 0.1;
    private double fullOffsetThreshold = 0.2;
    private double nettyDelayThreshold = 40.0;
    private static final double BUFFER_CAP = 6.0;

    public CrossNoFall(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        offsetThreshold     = config.getDoubleElse(base + "offset-threshold", 0.1);
        fullOffsetThreshold = config.getDoubleElse(base + "full-offset-threshold", 0.2);
        nettyDelayThreshold = config.getDoubleElse(base + "netty-delay-threshold", 40.0);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.inVehicle() || player.compensatedEntities.self.isDead) return;
        if (player.packetStateData.lastPacketWasTeleport) return;

        if (player.wasTouchingWater || player.wasTouchingLava
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.LEVITATION)
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.SLOW_FALLING)) {
            reward();
            return;
        }

        double yOffset = Math.abs(player.crossValidationData.pePositionDeltaY
            - player.crossValidationData.predictedDeltaY);
        double fullOffset = player.crossValidationData.offsetFromPrediction;

        // Require BOTH Y offset AND full offset to be suspicious.
        // A high Y delta alone is often just step-up / slab physics.
        boolean groundSpoof = player.crossValidationData.peOnGround
            && yOffset > offsetThreshold && fullOffset > fullOffsetThreshold;

        if (!groundSpoof) {
            buffer = Math.max(0, buffer - 0.2);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyAvgDelayBetweenPacketsMs < nettyDelayThreshold;
        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "NoFall");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        int ping = player.getTransactionPing();
        double multiplier = ping > 400 ? 0.5 : 1.0;

        if (spartanConfirms && nettyConfirms) {
            buffer = Math.min(BUFFER_CAP, buffer + 1.5 * multiplier);
            if (buffer > 5.0) {
                flagAndAlert(String.format("yOff=%.3f off=%.3f netty=%.1fms spartan=%s",
                    yOffset, fullOffset,
                    player.crossValidationData.nettyAvgDelayBetweenPacketsMs, spartanResult.type()));
                buffer = 0;
                return;
            }
        } else if (spartanConfirms || nettyConfirms) {
            buffer = Math.min(BUFFER_CAP, buffer + 0.8 * multiplier);
        } else {
            buffer = Math.max(0, buffer - 0.2);
        }
    }
}
