package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsZ", stableKey = "sac.badpackets.duplicate_player_input")
public class BadPacketsZ extends Check implements PacketCheck {
    private boolean sent;

    public BadPacketsZ(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END) {
            sent = false;
        }

        // Clients older than 1.21.2 do not send CLIENT_TICK_END, so reset on flying
        // packets instead. 1.21.2+ clients are excluded here (isOlderThan, not
        // isOlderThanOrEquals) so 1.21.2 is not double-reset by both branches.
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_21_2)
                && WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            sent = false;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_INPUT) {
            if (sent) {
                flagAndAlert();
            } else {
                reward();
            }

            sent = true;
        }
    }
}
