package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;

@CheckData(name = "BadPacketsAH", stableKey = "sac.badpackets.invalid_spectate", description = "Detects spectate teleport while not spectating", setback = 5)
public class BadPacketsAH extends Check implements PacketCheck {

    public BadPacketsAH(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.SPECTATE) return;
        if (player.gamemode != GameMode.SPECTATOR) {
            flagAndAlert("gamemode=" + player.gamemode);
        }
    }
}
