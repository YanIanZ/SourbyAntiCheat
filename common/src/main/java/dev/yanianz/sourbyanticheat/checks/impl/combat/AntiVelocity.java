package dev.yanianz.sourbyanticheat.checks.impl.combat;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "AntiVelocity", stableKey = "sac.combat.antivelocity", description = "Detects anti-knockback hacks", setback = 15, decay = 0.01, experimental = true)
public class AntiVelocity extends Check implements PacketCheck {

    private double lastVelocityX, lastVelocityZ;
    private int flags = 0;

    public AntiVelocity(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (!event.getPacketType().toString().contains("POSITION")) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.inVehicle() || player.isFlying || player.canFly) return;

        double deltaX = Math.abs(player.x - player.lastX);
        double deltaZ = Math.abs(player.z - player.lastZ);

        if (deltaX < 0.001 && deltaZ < 0.001 && (lastVelocityX > 0.01 || lastVelocityZ > 0.01)) {
            flags++;
            if (flags > 5) {
                flagAndAlert("zero_movement flags=" + flags);
            }
        } else {
            flags = Math.max(0, flags - 1);
            if (flags < 2) reward();
        }

        lastVelocityX = deltaX;
        lastVelocityZ = deltaZ;
    }
}
