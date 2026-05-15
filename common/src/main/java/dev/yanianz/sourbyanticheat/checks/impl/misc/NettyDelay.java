package dev.yanianz.sourbyanticheat.checks.impl.misc;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "NettyDelay", stableKey = "sac.netty.delay", description = "Detects timer via Netty packet spacing analysis", setback = 15, decay = 0.02, experimental = true)
public class NettyDelay extends Check implements PacketCheck {

    private long lastPacketTime = 0;
    private double delayBuffer = 0;
    private static final double MIN_DELAY_NS = 40_000_000.0;

    public NettyDelay(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;

        long now = System.nanoTime();
        if (lastPacketTime == 0) { lastPacketTime = now; return; }

        long delta = now - lastPacketTime;
        lastPacketTime = now;

        if (delta < MIN_DELAY_NS) {
            delayBuffer += (MIN_DELAY_NS - delta) / 1_000_000.0;
            if (delayBuffer > 100) {
                flagAndAlert("tick_delta=" + (delta / 1_000_000) + "ms buffer=" + String.format("%.1f", delayBuffer));
            }
        } else {
            delayBuffer = Math.max(0, delayBuffer - 1);
            if (delayBuffer < 1) reward();
        }
    }
}
