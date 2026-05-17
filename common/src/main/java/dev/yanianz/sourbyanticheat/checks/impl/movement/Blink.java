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
    private double lastX, lastY, lastZ;
    private int blinkCount = 0;

    public Blink(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        if (player.packetStateData.lastPacketWasTeleport
                || player.inVehicle() || player.canFly || player.isGliding
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) {
            blinkCount = 0;
            return;
        }

        long now = System.currentTimeMillis();
        if (lastPacketTime > 0) {
            long gap = now - lastPacketTime;
            double moved = Math.sqrt(
                Math.pow(player.x - lastX, 2)
                + Math.pow(player.y - lastY, 2)
                + Math.pow(player.z - lastZ, 2)
            );

            if (gap > 2000 && moved > 3.0) {
                blinkCount++;
                if (blinkCount > 3) {
                    flagAndAlert("gap=" + gap + "ms moved=" + String.format("%.1f", moved) + " count=" + blinkCount);
                }
            } else {
                blinkCount = Math.max(0, blinkCount - 1);
                if (blinkCount < 2) reward();
            }
        }
        lastPacketTime = now;
        lastX = player.x;
        lastY = player.y;
        lastZ = player.z;
    }
}
