package dev.yanianz.sourbyanticheat.checks.impl.crash;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings;

@CheckData(name = "CrashE", stableKey = "sac.crash.low_view_distance")
public class CrashE extends Check implements PacketCheck {

    public CrashE(SacPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_SETTINGS) {
            WrapperPlayClientSettings wrapper = new WrapperPlayClientSettings(event);
            int viewDistance = wrapper.getViewDistance();
            if (viewDistance < 2) {
                flagAndAlert("distance=" + viewDistance);
                wrapper.setViewDistance(2);
            }
        }
    }

}
