package dev.yanianz.sourbyanticheat.checks.impl.movement;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "Jesus", stableKey = "sac.movement.jesus", description = "Detects water walking / jesus hacks", setback = 10, decay = 0.02)
public class Jesus extends Check implements PacketCheck {

    private int surfaceTicks = 0;
    private double lastY = 0;

    public Jesus(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.canFly || player.isFlying || player.isGliding || player.inVehicle()) return;

        WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
        double deltaY = player.y - lastY;
        double frac = player.y - Math.floor(player.y);

        boolean nearSurface = frac > 0.85 && frac < 0.99;
        boolean notFalling = deltaY > -0.005 && !flying.isOnGround();

        if (nearSurface && notFalling) {
            surfaceTicks++;
            if (surfaceTicks > 15) {
                flagAndAlert("surface=" + String.format("%.3f", frac) + " dY=" + String.format("%.3f", deltaY));
            }
        } else {
            surfaceTicks = Math.max(0, surfaceTicks - 2);
            if (surfaceTicks < 5) reward();
        }

        lastY = player.y;
    }
}
