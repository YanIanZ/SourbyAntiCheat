package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossTeleport", configName = "crossteleport", decay = 0.02, setback = 10, stableKey = "cross.teleport")
public class CrossTeleport extends Check implements PostPredictionCheck {

    private int buffer;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double teleportDistThreshold = 8.0;
    private double nettyRateThreshold    = 18.0;

    public CrossTeleport(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        teleportDistThreshold = config.getDoubleElse(base + "teleport-dist-threshold", 8.0);
        nettyRateThreshold    = config.getDoubleElse(base + "netty-rate-threshold", 18.0);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        // Legitimate large server-side deltas: a teleport packet (also covers ender-pearl,
        // chorus-fruit and bed-respawn teleports — the server teleports the player in all
        // three cases) or a recent teleport whose flag has already cleared. Riptide launches
        // also produce a legitimate large single-tick delta.
        if (player.packetStateData.lastPacketWasTeleport
                || player.uncertaintyHandler.lastTeleportTicks.hasOccurredSince(2)
                || player.isInBed || player.lastInBed
                || player.riptideSpinAttackTicks > 0) {
            buffer = Math.max(0, buffer - 1);
            reward();
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

        if (dist < teleportDistThreshold) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec < nettyRateThreshold;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Teleport");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 2) {
            flagAndAlert(String.format("dist=%.1f netty=%.1f/s spartan=%s",
                dist, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
