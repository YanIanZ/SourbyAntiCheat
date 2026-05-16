package dev.yanianz.sourbyanticheat.checks.impl.movement;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "SafeWalk", stableKey = "sac.movement.safewalk", description = "Detects SafeWalk / edge walk hacks", setback = 5, decay = 0.02)
public class SafeWalk extends Check implements PacketCheck {

    private int stopTicks = 0;
    private double lastDeltaX, lastDeltaZ;

    public SafeWalk(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.canFly || player.isFlying || player.isGliding || player.inVehicle()) return;

        double deltaX = Math.abs(player.x - player.lastX);
        double deltaZ = Math.abs(player.z - player.lastZ);

        if (lastDeltaX > 0.05 && deltaX < 0.001) {
            stopTicks++;
            if (stopTicks > 10) {
                flagAndAlert("sudden_stop ticks=" + stopTicks);
            }
        } else {
            stopTicks = Math.max(0, stopTicks - 1);
            if (stopTicks < 2) reward();
        }

        lastDeltaX = deltaX;
        lastDeltaZ = deltaZ;
    }
}
