package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "Freecam", configName = "freecam", decay = 0.05, setback = 10, stableKey = "cross.freecam")
public class Freecam extends Check implements PostPredictionCheck {

    private int buffer;
    private long lastChunkAck = System.currentTimeMillis();
    private double anchorX, anchorY, anchorZ;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private long chunkAckTimeoutMs = 3000L;
    private double moveThreshold = 20.0;
    private double velocityThreshold = 30.0;
    private static final double NETTY_RATE_THRESHOLD = 120.0;

    public Freecam(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        chunkAckTimeoutMs = config.getIntElse(base + "chunk-ack-timeout-ms", 3000);
        moveThreshold     = config.getDoubleElse(base + "move-threshold", 20.0);
        velocityThreshold = config.getDoubleElse(base + "velocity-threshold", 30.0);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CHUNK_BATCH_ACK) {
            lastChunkAck = System.currentTimeMillis();
            anchorX = player.x;
            anchorY = player.y;
            anchorZ = player.z;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        // CHUNK_BATCH_ACK is a 1.20.2+ packet. Clients older than that — including
        // ViaVersion-translated clients — never send it, so lastChunkAck never
        // advances and chunkGap grows unbounded. The chunk-gap signal is meaningless
        // for them; skip the check entirely.
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_20_2)) return;

        if (player.packetStateData.lastPacketWasTeleport) {
            buffer = Math.max(0, buffer - 1);
            // A teleport reloads chunks — reset the anchor and gap so they don't
            // carry a stale value across the teleport.
            lastChunkAck = System.currentTimeMillis();
            anchorX = player.x;
            anchorY = player.y;
            anchorZ = player.z;
            return;
        }
        if (player.inVehicle() || player.isGliding || player.canFly || player.isFlying
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        double tickDist = Math.sqrt(
            Math.pow(player.x - player.lastX, 2)
            + Math.pow(player.y - player.lastY, 2)
            + Math.pow(player.z - player.lastZ, 2)
        );
        boolean velocityFlag = tickDist > velocityThreshold;

        long now = System.currentTimeMillis();
        long chunkGap = now - lastChunkAck;
        double distFromAnchor = Math.sqrt(
            Math.pow(player.x - anchorX, 2)
            + Math.pow(player.y - anchorY, 2)
            + Math.pow(player.z - anchorZ, 2)
        );
        boolean chunkFlag = chunkGap > chunkAckTimeoutMs && distFromAnchor > moveThreshold;

        // chunkFlag now genuinely contributes: either a raw velocity spike, or moving
        // far from the last acked anchor while still drifting (tickDist > 5).
        boolean flag = velocityFlag || (chunkFlag && tickDist > 5.0);

        if (!flag) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "Freecam");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 4) {
            flagAndAlert(String.format("vDist=%.1f chunkGap=%dms anchorDist=%.1f netty=%.1f/s spartan=%s",
                tickDist, chunkGap, distFromAnchor,
                player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
