// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

/**
 * Detects impossible/spoofed ability packets — clients sending ability packets
 * (flying toggle, creative mode toggle) that don't match the server's allowed state.
 * <p>
 * Vanilla clients only send PLAYER_ABILITIES when the server enables flying.
 * Cheat clients send spoofed ability packets to enable fly without permission.
 */
@CheckData(name = "BadPacketsAI", stableKey = "sac.badpackets.ai", description = "Detects spoofed ability packets", setback = 20, decay = 0.01)
public class BadPacketsAI extends Check implements PacketCheck {

    private boolean serverAllowsFlight = false;

    public BadPacketsAI(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_ABILITIES) {
            var packet = new com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerAbilities(event);
            serverAllowsFlight = packet.isFlightAllowed();
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_ABILITIES) return;

        var packet = new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerAbilities(event);

        // If the client says they are flying but the server hasn't allowed flight
        if (packet.isFlying() && !serverAllowsFlight && !player.canFly) {
            flagAndAlert("spoofed_fly");
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }
}
