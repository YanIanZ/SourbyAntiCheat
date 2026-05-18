package dev.yanianz.sourbyanticheat.checks.impl.movement;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "EntitySpeed", stableKey = "sac.movement.entityspeed", description = "Detects speed hacks while riding entities", setback = 10, decay = 0.02)
public class EntitySpeed extends Check implements PacketCheck {

    private double speedBuffer = 0;

    // Per-entity-type speed limits (config-wired, defaults equal prior single ceiling)
    private double maxSpeedPig      = 0.50;
    private double maxSpeedHorse    = 0.50;
    private double maxSpeedStrider  = 0.50;
    private double maxSpeedBoat     = 0.50;
    private double maxSpeedMinecart = 0.50;
    private double maxSpeedDefault  = 0.50;
    private double bufferIncrement  = 0.5;
    private double bufferDecay      = 0.01;

    public EntitySpeed(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.maxSpeedPig      = config.getDoubleElse(base + "max-speed-pig",      0.50);
        this.maxSpeedHorse    = config.getDoubleElse(base + "max-speed-horse",    0.50);
        this.maxSpeedStrider  = config.getDoubleElse(base + "max-speed-strider",  0.50);
        this.maxSpeedBoat     = config.getDoubleElse(base + "max-speed-boat",     0.50);
        this.maxSpeedMinecart = config.getDoubleElse(base + "max-speed-minecart", 0.50);
        this.maxSpeedDefault  = config.getDoubleElse(base + "max-speed-default",  0.50);
        this.bufferIncrement  = config.getDoubleElse(base + "buffer-increment",   0.5);
        this.bufferDecay      = config.getDoubleElse(base + "buffer-decay",       0.01);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (!player.inVehicle()) return;
        // Teleport-after-mount grace — a teleport (including the mount sync) produces a large
        // position delta unrelated to vehicle speed.
        if (player.packetStateData.lastPacketWasTeleport) return;

        // Server-push exemption — knockback/explosion velocity legitimately accelerates the mount.
        if (player.likelyKB != null || player.likelyExplosions != null
                || player.firstBreadKB != null || player.firstBreadExplosion != null) {
            speedBuffer = Math.max(0, speedBuffer - bufferDecay);
            reward();
            return;
        }

        // Speed-effect / levitation exemption on the rider — these boost mount speed legitimately.
        if (player.compensatedEntities.self.hasPotionEffect(PotionTypes.SPEED)
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.LEVITATION)) {
            speedBuffer = Math.max(0, speedBuffer - bufferDecay);
            reward();
            return;
        }

        double deltaX = player.x - player.lastX;
        double deltaZ = player.z - player.lastZ;
        double deltaH = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        double limit = getSpeedLimitForVehicle(player.getVehicleType());

        // Scale the limit by the ridden entity's MOVEMENT_SPEED attribute — plugin-spawned fast
        // mounts (or attribute-modified ones) are legitimately faster than the vanilla base.
        PacketEntity vehicle = player.getVehicle();
        if (vehicle != null) {
            double baseSpeed = getBaseSpeedForVehicle(vehicle.type);
            if (baseSpeed > 0) {
                double actualVehicleSpeed = vehicle.getAttribute(Attributes.MOVEMENT_SPEED)
                        .map(attr -> attr.get())
                        .orElse(baseSpeed);
                if (actualVehicleSpeed > baseSpeed) {
                    limit = limit * (actualVehicleSpeed / baseSpeed);
                }
            }
        }

        if (deltaH > limit) {
            speedBuffer += deltaH - limit;
            if (speedBuffer > bufferIncrement) {
                flagAndAlert("h=" + String.format("%.3f", deltaH)
                    + " limit=" + String.format("%.3f", limit)
                    + " buf=" + String.format("%.3f", speedBuffer));
            }
        } else {
            speedBuffer = Math.max(0, speedBuffer - bufferDecay);
            reward();
        }
    }

    private double getSpeedLimitForVehicle(EntityType type) {
        if (type == null) return maxSpeedDefault;
        if (type == EntityTypes.PIG) return maxSpeedPig;
        if (EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_HORSE)) return maxSpeedHorse;
        if (type == EntityTypes.STRIDER) return maxSpeedStrider;
        if (EntityTypes.isTypeInstanceOf(type, EntityTypes.BOAT)) return maxSpeedBoat;
        if (type == EntityTypes.MINECART) return maxSpeedMinecart;
        return maxSpeedDefault;
    }

    /**
     * Returns the default (vanilla base) MOVEMENT_SPEED attribute value for the given vehicle
     * entity type. Returns 0 for vehicles that do not track MOVEMENT_SPEED (boats, minecarts) —
     * callers must guard against a zero return value.
     */
    private double getBaseSpeedForVehicle(EntityType type) {
        if (type == null) return 0;
        if (type == EntityTypes.PIG) return 0.1;
        if (type == EntityTypes.STRIDER) return 0.1;
        if (EntityTypes.isTypeInstanceOf(type, EntityTypes.CHESTED_HORSE)) return 0.175;
        if (type == EntityTypes.CAMEL) return 0.09;
        if (type == EntityTypes.ZOMBIE_HORSE || type == EntityTypes.SKELETON_HORSE) return 0.2;
        if (EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_HORSE)) return 0.225;
        return 0;
    }
}
