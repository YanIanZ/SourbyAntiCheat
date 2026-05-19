package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;

@CheckData(name = "CrossTower", configName = "crosstower", decay = 0.02, setback = 10, stableKey = "cross.tower")
public class CrossTower extends Check implements PostPredictionCheck {

    private double buffer;
    private int consecutiveUpTicks;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double towerYThreshold     = 0.3;
    private double nettyRateThreshold  = 120.0;

    public CrossTower(SacPlayer player) { super(player); }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        towerYThreshold    = config.getDoubleElse(base + "tower-y-threshold",    0.3);
        nettyRateThreshold = config.getDoubleElse(base + "netty-rate-threshold", 120.0);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.compensatedEntities.self.isDead) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.inVehicle() || player.canFly || player.isGliding
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.JUMP_BOOST)
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.LEVITATION)) return;

        double deltaY = player.crossValidationData.pePositionDeltaY;

        if (deltaY > towerYThreshold) {
            consecutiveUpTicks++;
        } else {
            consecutiveUpTicks = Math.max(0, consecutiveUpTicks - 1);
            buffer = Math.max(0, buffer - 0.02);
            reward();
            return;
        }

        if (consecutiveUpTicks < 3) return;

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > nettyRateThreshold;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "Tower");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        int ping = player.getTransactionPing();
        double multiplier = ping > 400 ? 0.5 : 1.0;
        buffer += ((nettyConfirms || spartanConfirms) ? 1.5 : 0.5) * multiplier;
        if (buffer > 3.0) {
            flagAndAlert(String.format("dY=%.3f ticks=%d netty=%.1f/s spartan=%s",
                deltaY, consecutiveUpTicks, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
