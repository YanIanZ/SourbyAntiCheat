package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsV", stableKey = "sac.badpackets.slow_move", description = "Did not move far enough", experimental = true)
public class BadPacketsV extends Check implements PacketCheck {
    private int noReminderTicks;

    public BadPacketsV(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!player.canSkipTicks() && isTickPacket(event.getPacketType())) {
            if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
                int positionAtLeastEveryNTicks = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8) ? 20 : 19;

                if (noReminderTicks < positionAtLeastEveryNTicks && !player.uncertaintyHandler.lastTeleportTicks.hasOccurredSince(1)) {
                    final double deltaSq = new WrapperPlayClientPlayerFlying(event).getLocation().getPosition()
                            .distanceSquared(new Vector3d(player.lastX, player.lastY, player.lastZ));
                    if (deltaSq <= player.getMovementThreshold() * player.getMovementThreshold()) {
                        flagAndAlert("delta=" + Math.sqrt(deltaSq));
                    }
                }

                noReminderTicks = 0;
            } else {
                noReminderTicks++;
            }
        }
    }
}
