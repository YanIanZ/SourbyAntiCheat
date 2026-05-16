package dev.yanianz.sourbyanticheat.checks.impl.movement;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "Blink", stableKey = "sac.movement.blink", description = "Detects blink/fakelag hacks", setback = 15, decay = 0.02)
public class Blink extends Check implements PacketCheck {

    private long lastPacketTime = 0;
    private int blinkCount = 0;

    public Blink(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;

        long now = System.currentTimeMillis();
        if (lastPacketTime > 0) {
            long gap = now - lastPacketTime;
            if (gap > 500) {
                blinkCount++;
                if (blinkCount > 5) {
                    flagAndAlert("gap=" + gap + "ms count=" + blinkCount);
                }
            } else {
                blinkCount = Math.max(0, blinkCount - 1);
                if (blinkCount < 2) reward();
            }
        }
        lastPacketTime = now;
    }
}
