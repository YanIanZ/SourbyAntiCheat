package dev.yanianz.sourbyanticheat.checks.impl.movement;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "NoWeb", stableKey = "sac.movement.noweb", description = "Detects ignoring cobweb slowdown", setback = 10, decay = 0.02)
public class NoWeb extends Check implements PacketCheck {

    private static final double MAX_WEB_SPEED = 0.08;
    private double webBuffer = 0;

    public NoWeb(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.canFly || player.isFlying || player.isGliding || player.inVehicle()) return;

        double deltaX = Math.abs(player.x - player.lastX);
        double deltaZ = Math.abs(player.z - player.lastZ);
        double deltaH = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (deltaH > MAX_WEB_SPEED && deltaH < 0.5) {
            webBuffer += deltaH - MAX_WEB_SPEED;
            if (webBuffer > 0.15) {
                flagAndAlert("h=" + String.format("%.3f", deltaH) + " buf=" + String.format("%.3f", webBuffer));
            }
        } else {
            webBuffer = Math.max(0, webBuffer - 0.005);
            if (webBuffer < 0.01) reward();
        }
    }
}
