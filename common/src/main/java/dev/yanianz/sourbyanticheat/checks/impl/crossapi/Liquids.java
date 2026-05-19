package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "Liquids", configName = "liquids", decay = 0.05, setback = 15, stableKey = "cross.liquids")
public class Liquids extends Check implements PostPredictionCheck {

    private double buffer;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double offsetThreshold = 0.15;
    private double nettyRateThreshold = 120.0;

    public Liquids(SacPlayer player) { super(player); }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        offsetThreshold    = config.getDoubleElse(base + "offset-threshold", 0.15);
        nettyRateThreshold = config.getDoubleElse(base + "netty-rate-threshold", 120.0);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.compensatedEntities.self.isDead) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.inVehicle() || player.canFly || player.isGliding) return;

        double offset = player.crossValidationData.offsetFromPrediction;
        boolean inLiquid = player.wasTouchingWater || player.wasTouchingLava;
        boolean liquidWalk = inLiquid && player.crossValidationData.pePositionDeltaY >= -0.01 && offset > offsetThreshold;

        if (!liquidWalk) { buffer = Math.max(0, buffer - 0.05); reward(); return; }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > nettyRateThreshold;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "Jesus");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        int ping = player.getTransactionPing();
        double multiplier = ping > 400 ? 0.5 : 1.0;
        buffer += ((nettyConfirms || spartanConfirms) ? 1.5 : 0.5) * multiplier;
        if (buffer > 3.0) {
            flagAndAlert(String.format("offset=%.3f lava=%b netty=%.1f/s spartan=%s",
                offset, player.wasTouchingLava,
                player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
