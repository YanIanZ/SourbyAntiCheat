package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossTeleport", configName = "crossteleport", decay = 0.02, setback = 10, stableKey = "cross.teleport")
public class CrossTeleport extends Check implements PostPredictionCheck {

    private int buffer;
    private static final double TELEPORT_DIST_THRESHOLD = 8.0;
    private static final double NETTY_RATE_THRESHOLD = 18.0;

    public CrossTeleport(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        if (player.packetStateData.lastPacketWasTeleport) {
            buffer = Math.max(0, buffer - 1);
            return;
        }
        if (player.inVehicle() || player.canFly
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        double dist = Math.sqrt(
            Math.pow(player.x - player.lastX, 2)
            + Math.pow(player.y - player.lastY, 2)
            + Math.pow(player.z - player.lastZ, 2)
        );

        if (dist < TELEPORT_DIST_THRESHOLD) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec < NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Phase");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 2) {
            flagAndAlert(String.format("dist=%.1f netty=%.1f/s spartan=%s",
                dist, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
