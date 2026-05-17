package dev.yanianz.sourbyanticheat.checks.impl.combat;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;

@CheckData(name = "AntiVelocity", stableKey = "sac.combat.antivelocity", description = "Detects anti-knockback via velocity packet tracking", setback = 15, decay = 0.01)
public class AntiVelocity extends Check implements PacketCheck {

    private double pendingVelX = 0;
    private double pendingVelZ = 0;
    private boolean velocityPending = false;
    private int ticksSinceVelocity = 0;
    private double totalActualMovement = 0;
    private int buffer = 0;

    private static final int RESPONSE_TICKS = 6;
    private static final double MIN_VELOCITY = 0.01;
    private static final double GROUND_FRICTION = 0.6;
    private static final double AIR_FRICTION = 0.91;

    public AntiVelocity(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_VELOCITY) return;

        WrapperPlayServerEntityVelocity velocity = new WrapperPlayServerEntityVelocity(event);
        if (velocity.getEntityId() != player.entityID) return;

        double vx = velocity.getVelocity().getX();
        double vz = velocity.getVelocity().getZ();
        double magnitude = Math.sqrt(vx * vx + vz * vz);

        if (magnitude > MIN_VELOCITY) {
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                pendingVelX = vx;
                pendingVelZ = vz;
                velocityPending = true;
                ticksSinceVelocity = 0;
                totalActualMovement = 0;
            });
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.inVehicle() || player.isFlying || player.canFly || player.isGliding) return;

        if (player.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
            velocityPending = false;
            return;
        }

        if (!velocityPending) return;

        ticksSinceVelocity++;

        if (ticksSinceVelocity >= 2 && ticksSinceVelocity <= RESPONSE_TICKS) {
            double deltaX = player.x - player.lastX;
            double deltaZ = player.z - player.lastZ;
            totalActualMovement += Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        }

        if (ticksSinceVelocity > RESPONSE_TICKS) {
            double friction = player.onGround ? GROUND_FRICTION : AIR_FRICTION;
            double expected = cumulativeFrictionMovement(Math.sqrt(pendingVelX * pendingVelX + pendingVelZ * pendingVelZ), friction, RESPONSE_TICKS - 1);

            double ratio = expected > 0.001 ? totalActualMovement / expected : 1.0;

            if (ratio < 0.08) {
                buffer++;
                if (buffer > 2) {
                    flagAndAlert("ratio=" + String.format("%.3f", ratio) + " actual=" + String.format("%.4f", totalActualMovement) + " expected=" + String.format("%.4f", expected));
                }
            } else {
                buffer = Math.max(0, buffer - 1);
                if (buffer < 2) reward();
            }

            velocityPending = false;
        }
    }

    private static double cumulativeFrictionMovement(double initial, double friction, int ticks) {
        double total = 0;
        double vel = initial;
        for (int i = 0; i < ticks; i++) {
            total += vel;
            vel *= friction;
        }
        return total;
    }
}
