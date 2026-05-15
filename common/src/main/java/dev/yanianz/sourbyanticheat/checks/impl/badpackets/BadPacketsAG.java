package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction;

@CheckData(name = "BadPacketsAG", stableKey = "sac.badpackets.invalid_creative_action", description = "Detects invalid creative inventory actions")
public class BadPacketsAG extends Check implements PacketCheck {

    public BadPacketsAG(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) return;
        if (player.gamemode == GameMode.CREATIVE) return;

        WrapperPlayClientCreativeInventoryAction packet = new WrapperPlayClientCreativeInventoryAction(event);
        if (packet.getSlot() < -1 || packet.getSlot() > 45) {
            flagAndAlert("slot=" + packet.getSlot() + " gamemode=" + player.gamemode);
        }
    }
}
