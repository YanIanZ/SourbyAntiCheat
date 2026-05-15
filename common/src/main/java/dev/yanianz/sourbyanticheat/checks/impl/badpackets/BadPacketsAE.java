package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

@CheckData(name = "BadPacketsAE", stableKey = "sac.badpackets.invalid_entity_interact", description = "Detects interacting with non-existent entities", setback = 5, decay = 0.01, experimental = true)
public class BadPacketsAE extends Check implements PacketCheck {

    private int invalidCount = 0;

    public BadPacketsAE(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;
        if (player.inVehicle()) return;

        invalidCount++;
        if (invalidCount > 20) {
            flagAndAlert("invalid_interacts=" + invalidCount);
        }
    }
}
