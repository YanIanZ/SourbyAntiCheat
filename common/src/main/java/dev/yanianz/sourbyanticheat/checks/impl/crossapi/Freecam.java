package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;

@CheckData(name = "Freecam", configName = "freecam", decay = 0.05, setback = 10, stableKey = "cross.freecam")
public class Freecam extends Check implements PacketCheck {

    private int buffer;
    private long lastChunkAck = System.currentTimeMillis();
    private double anchorX, anchorY, anchorZ;
    private boolean anchorSet = false;
    private static final long CHUNK_ACK_TIMEOUT_MS = 3000;
    private static final double MOVE_THRESHOLD = 10.0;

    public Freecam(SacPlayer player) {
        super(player);
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

        if (!anchorSet) return;

        long now = System.currentTimeMillis();
        long chunkGap = now - lastChunkAck;
        double distFromAnchor = Math.sqrt(
            Math.pow(player.x - anchorX, 2)
            + Math.pow(player.y - anchorY, 2)
            + Math.pow(player.z - anchorZ, 2)
        );

        boolean noChunkForMovement = chunkGap > CHUNK_ACK_TIMEOUT_MS && distFromAnchor > MOVE_THRESHOLD;

        if (noChunkForMovement) {
            buffer++;
            if (buffer > 3) {
                flagAndAlertWithSetback(String.format("dist=%.1f chunkGap=%dms",
                    distFromAnchor, chunkGap));
            }
        } else {
            buffer = Math.max(0, buffer - 1);
            reward();
        }
    }
}
