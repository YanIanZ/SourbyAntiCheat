package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossNoFall", configName = "crossnofall", decay = 0.15, setback = 15, stableKey = "cross.nofall")
public class CrossNoFall extends Check implements PostPredictionCheck {

    private double buffer;
    private static final double OFFSET_THRESHOLD = 0.06;
    private static final double NETTY_DELAY_THRESHOLD = 40.0;

    public CrossNoFall(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.inVehicle() || player.compensatedEntities.self.isDead) return;
        if (player.packetStateData.lastPacketWasTeleport) return;

        if (player.wasTouchingWater
                || player.compensatedEntities.self.hasPotionEffect(com.github.retrooper.packetevents.protocol.potion.PotionTypes.LEVITATION)
                || player.compensatedEntities.self.hasPotionEffect(com.github.retrooper.packetevents.protocol.potion.PotionTypes.SLOW_FALLING)) {
            reward();
            return;
        }

        double yOffset = Math.abs(player.crossValidationData.pePositionDeltaY
            - player.crossValidationData.predictedDeltaY);
        double fullOffset = player.crossValidationData.offsetFromPrediction;
        boolean groundSpoof = player.crossValidationData.peOnGround
            && (yOffset > OFFSET_THRESHOLD || fullOffset > 0.2);

        if (!groundSpoof) {
            buffer = Math.max(0, buffer - 0.15);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyAvgDelayBetweenPacketsMs < NETTY_DELAY_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "NoFall");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        int ping = player.getTransactionPing();
        double multiplier = ping > 400 ? 0.5 : 1.0;

        if (nettyConfirms || spartanConfirms) {
            buffer += 1.5 * multiplier;
            if (buffer > 5.0) {
                flagAndAlert(String.format("yOff=%.3f off=%.3f netty=%.1fms spartan=%s",
                    yOffset, fullOffset,
                    player.crossValidationData.nettyAvgDelayBetweenPacketsMs, spartanResult.type()));
            }
        } else {
            buffer += 0.5 * multiplier;
            if (buffer > 6.0) {
                flagAndAlert(String.format("yOff=%.3f off=%.3f (no cross-confirm)",
                    yOffset, fullOffset));
            }
        }

        reward();
    }
}
