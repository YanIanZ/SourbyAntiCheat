package dev.yanianz.sourbyanticheat.checks.impl.misc;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "NettyFlood", stableKey = "sac.netty.flood", description = "Detects packet flooding via cancelled packet rate", setback = 15)
public class NettyFlood extends Check implements PacketCheck {

    private static final int MAX_CANCELLED_PER_TICK = 10;

    public NettyFlood(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        int cancelled = player.cancelledPackets.get();
        if (cancelled > MAX_CANCELLED_PER_TICK) {
            flagAndAlert("cancelled=" + cancelled + "/tick");
        } else {
            reward();
        }
    }
}
