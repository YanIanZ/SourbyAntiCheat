package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "FastFall", configName = "fastfall", decay = 0.02, setback = 10, stableKey = "cross.fastfall")
public class FastFall extends Check implements PostPredictionCheck {

    private double buffer;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double fallThreshold       = 0.15;
    private double nettyRateThreshold  = 120.0;

    public FastFall(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        fallThreshold      = config.getDoubleElse(base + "fall-threshold",      0.15);
        nettyRateThreshold = config.getDoubleElse(base + "netty-rate-threshold", 120.0);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.compensatedEntities.self.isDead) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.inVehicle() || player.isGliding || player.canFly) return;
        if (player.wasTouchingLava
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.SLOW_FALLING)) return;

        double deltaY = player.crossValidationData.pePositionDeltaY;
        double predictedY = player.crossValidationData.predictedDeltaY;
        double fallExcess = Math.abs(deltaY) - Math.abs(predictedY);

        boolean fastFalling = deltaY < -0.1 && fallExcess > fallThreshold;

        if (!fastFalling) {
            buffer = Math.max(0, buffer - 0.02);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > nettyRateThreshold;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "NoFall");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        int ping = player.getTransactionPing();
        double multiplier = ping > 400 ? 0.5 : 1.0;
        buffer += ((nettyConfirms || spartanConfirms) ? 1.5 : 0.5) * multiplier;
        if (buffer > 3.0) {
            flagAndAlert(String.format("dY=%.3f predY=%.3f excess=%.3f netty=%.1f/s spartan=%s",
                deltaY, predictedY, fallExcess,
                player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
