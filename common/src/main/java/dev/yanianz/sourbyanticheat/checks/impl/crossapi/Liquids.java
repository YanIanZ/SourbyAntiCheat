package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "Liquids", configName = "liquids", decay = 0.05, setback = 15, stableKey = "cross.liquids")
public class Liquids extends Check implements PostPredictionCheck {

    private double buffer;
    private static final double OFFSET_THRESHOLD = 0.5;
    private static final double NETTY_RATE_THRESHOLD = 20.0;

    public Liquids(SacPlayer player) { super(player); }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.compensatedEntities.self.isDead) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.inVehicle() || player.canFly || player.isGliding) return;

        double offset = player.crossValidationData.offsetFromPrediction;
        boolean liquidWalk = player.wasTouchingWater && player.crossValidationData.pePositionDeltaY >= -0.01 && offset > OFFSET_THRESHOLD;

        if (!liquidWalk) { buffer = Math.max(0, buffer - 0.05); reward(); return; }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "Jesus");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 1.5 : 0.5;
        if (buffer > 3.0) {
            flagAndAlert(String.format("offset=%.3f netty=%.1f/s spartan=%s",
                offset, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
