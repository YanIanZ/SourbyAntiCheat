package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "Freecam", configName = "freecam", decay = 0.05, setback = 10, stableKey = "cross.freecam")
public class Freecam extends Check implements PostPredictionCheck {

    private int buffer;
    private long lastChunkAck = System.currentTimeMillis();
    private double anchorX, anchorY, anchorZ;
    private static final long CHUNK_ACK_TIMEOUT_MS = 3000;
    private static final double MOVE_THRESHOLD = 20.0;
    private static final double VELOCITY_THRESHOLD = 30.0;

    public Freecam(SacPlayer player) {
        super(player);
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

        if (player.packetStateData.lastPacketWasTeleport) {
            buffer = Math.max(0, buffer - 1);
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
        boolean velocityFlag = tickDist > VELOCITY_THRESHOLD;

        long now = System.currentTimeMillis();
        long chunkGap = now - lastChunkAck;
        double distFromAnchor = Math.sqrt(
            Math.pow(player.x - anchorX, 2)
            + Math.pow(player.y - anchorY, 2)
            + Math.pow(player.z - anchorZ, 2)
        );
        boolean chunkFlag = chunkGap > CHUNK_ACK_TIMEOUT_MS && distFromAnchor > MOVE_THRESHOLD;

        boolean flag = velocityFlag || (velocityFlag && chunkFlag) || (chunkFlag && tickDist > 5.0);

        if (!flag) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        buffer += 2;
        if (buffer > 4) {
            flagAndAlert(String.format("vDist=%.1f chunkGap=%dms anchorDist=%.1f",
                tickDist, chunkGap, distFromAnchor));
        }
    }
}
