package dev.yanianz.sourbyanticheat.events.packets;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

public class PacketPlayerTick extends PacketListenerAbstract {

    public PacketPlayerTick() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public boolean isPreVia() {
        return true;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END) {
            SacPlayer player = SacAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null || player.getClientVersion().isOlderThan(ClientVersion.V_1_21_2))
                return;

            PacketWorldBorder border = player.checkManager.getPacketCheck(PacketWorldBorder.class);
            border.tickBorder();
        } else if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            SacPlayer player = SacAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2))
                return;

            PacketWorldBorder border = player.checkManager.getPacketCheck(PacketWorldBorder.class);
            border.tickBorder();
        }
    }

}
