package dev.yanianz.sourbyanticheat.checks.impl.movement;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "EntitySpeed", stableKey = "sac.movement.entityspeed", description = "Detects speed hacks while riding entities", setback = 10, decay = 0.02)
public class EntitySpeed extends Check implements PacketCheck {

    private static final double MAX_HORSE_SPEED = 0.50;
    private double speedBuffer = 0;

    public EntitySpeed(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (!player.inVehicle()) return;
        if (player.packetStateData.lastPacketWasTeleport) return;

        double deltaX = player.x - player.lastX;
        double deltaZ = player.z - player.lastZ;
        double deltaH = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (deltaH > MAX_HORSE_SPEED) {
            speedBuffer += deltaH - MAX_HORSE_SPEED;
            if (speedBuffer > 0.5) {
                flagAndAlert("h=" + String.format("%.3f", deltaH) + " buf=" + String.format("%.3f", speedBuffer));
            }
        } else {
            speedBuffer = Math.max(0, speedBuffer - 0.01);
            if (speedBuffer < 0.01) reward();
        }
    }
}
