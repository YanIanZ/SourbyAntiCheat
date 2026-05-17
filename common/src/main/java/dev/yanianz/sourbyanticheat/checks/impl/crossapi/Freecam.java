package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.collisions.datatypes.SimpleCollisionBox;
import dev.yanianz.sourbyanticheat.utils.nmsutil.Collisions;

import java.util.ArrayList;
import java.util.List;

@CheckData(name = "Freecam", configName = "freecam", decay = 0.05, setback = 10, stableKey = "cross.freecam")
public class Freecam extends Check implements PacketCheck {

    private int buffer;
    private long lastChunkAck = System.currentTimeMillis();
    private double anchorX, anchorY, anchorZ;
    private boolean anchorSet = false;
    private SimpleCollisionBox lastBox;
    private static final long CHUNK_ACK_TIMEOUT_MS = 2000;
    private static final double MOVE_THRESHOLD = 10.0;
    private static final double VELOCITY_THRESHOLD = 15.0;
    private static final double INSIDE_BLOCK_DIST = 5.0;

    public Freecam(SacPlayer player) {
        super(player);
        lastBox = player.boundingBox.copy();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (event.getPacketType() == PacketType.Play.Client.CHUNK_BATCH_ACK) {
            lastChunkAck = System.currentTimeMillis();
            anchorX = player.x;
            anchorY = player.y;
            anchorZ = player.z;
            anchorSet = true;
            return;
        }

        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        if (player.packetStateData.lastPacketWasTeleport) {
            anchorSet = false;
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

        SimpleCollisionBox newBox = player.boundingBox.copy();
        boolean insideBlock = false;
        if (tickDist > INSIDE_BLOCK_DIST) {
            List<SimpleCollisionBox> boxes = new ArrayList<>();
            Collisions.getCollisionBoxes(player, newBox, boxes, false);
            for (SimpleCollisionBox box : boxes) {
                if (newBox.isIntersected(box) && !lastBox.isIntersected(box)) {
                    insideBlock = true;
                    break;
                }
            }
        }
        lastBox = newBox;

        if (!anchorSet && !velocityFlag && !insideBlock) return;

        long now = System.currentTimeMillis();
        long chunkGap = now - lastChunkAck;
        double distFromAnchor = anchorSet ? Math.sqrt(
            Math.pow(player.x - anchorX, 2)
            + Math.pow(player.y - anchorY, 2)
            + Math.pow(player.z - anchorZ, 2)
        ) : 0;

        boolean noChunkForMovement = anchorSet && chunkGap > CHUNK_ACK_TIMEOUT_MS && distFromAnchor > MOVE_THRESHOLD;
        boolean flag = noChunkForMovement || velocityFlag || insideBlock;

        if (flag) {
            buffer += 2;
            if (buffer > 3) {
                String type = insideBlock ? "insideBlock" : velocityFlag ? "vDist=" + tickDist : "dist";
                flagAndAlertWithSetback(String.format("%s=%.1f chunkGap=%dms",
                    type,
                    insideBlock || velocityFlag ? tickDist : distFromAnchor,
                    chunkGap));
            }
        } else {
            buffer = Math.max(0, buffer - 1);
            reward();
        }
    }
}
