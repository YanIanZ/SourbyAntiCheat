package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsD", stableKey = "sac.badpackets.invalid_pitch", description = "Impossible pitch", decay = 0.01)
public class BadPacketsD extends Check implements PacketCheck {
    private static final float MAX_PITCH = 90.0f;

    public BadPacketsD(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.packetStateData.lastPacketWasTeleport) return;

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            final float pitch = new WrapperPlayClientPlayerFlying(event).getLocation().getPitch();
            if (Float.isNaN(pitch) || Float.isInfinite(pitch) || pitch > MAX_PITCH || pitch < -MAX_PITCH) {
                // Ban.
                if (flagAndAlert("pitch=" + pitch) && shouldModifyPackets()) {
                    // prevent other checks from using an invalid pitch — clamp based on the
                    // local (packet) pitch, not the stale player.pitch field
                    if (Float.isNaN(pitch) || Float.isInfinite(pitch)) {
                        player.pitch = 0;
                    } else if (pitch > MAX_PITCH) {
                        player.pitch = MAX_PITCH;
                    } else if (pitch < -MAX_PITCH) {
                        player.pitch = -MAX_PITCH;
                    }

                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            } else {
                reward();
            }
        }
    }
}
