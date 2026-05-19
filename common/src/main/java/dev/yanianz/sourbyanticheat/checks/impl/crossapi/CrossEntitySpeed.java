package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import dev.yanianz.sourbyanticheat.utils.data.packetentity.PacketEntity;

@CheckData(name = "CrossEntitySpeed", configName = "crossentityspeed", decay = 0.02, setback = 10, stableKey = "cross.entityspeed")
public class CrossEntitySpeed extends Check implements PostPredictionCheck {

    private double buffer;
    private static final double NETTY_RATE_THRESHOLD = 120.0;

    // Per-entity-type speed limits (config-wired)
    private double maxSpeedPig      = 0.35;
    private double maxSpeedHorse    = 0.35;
    private double maxSpeedStrider  = 0.35;
    private double maxSpeedBoat     = 0.35;
    private double maxSpeedMinecart = 0.35;
    private double maxSpeedDefault  = 0.35;

    public CrossEntitySpeed(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        maxSpeedPig      = config.getDoubleElse(base + "max-speed-pig",      0.35);
        maxSpeedHorse    = config.getDoubleElse(base + "max-speed-horse",    0.35);
        maxSpeedStrider  = config.getDoubleElse(base + "max-speed-strider",  0.35);
        maxSpeedBoat     = config.getDoubleElse(base + "max-speed-boat",     0.35);
        maxSpeedMinecart = config.getDoubleElse(base + "max-speed-minecart", 0.35);
        maxSpeedDefault  = config.getDoubleElse(base + "max-speed-default",  0.35);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.compensatedEntities.self.isDead) return;
        if (!player.inVehicle()) {
            reward();
            return;
        }

        // Speed-potion / levitation exemption — these effects boost mount speed legitimately
        if (player.compensatedEntities.self.hasPotionEffect(com.github.retrooper.packetevents.protocol.potion.PotionTypes.SPEED)
                || player.compensatedEntities.self.hasPotionEffect(com.github.retrooper.packetevents.protocol.potion.PotionTypes.LEVITATION)) {
            reward();
            return;
        }

        double deltaX = player.x - player.lastX;
        double deltaZ = player.z - player.lastZ;
        double speed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        double limit = getSpeedLimitForVehicle(player.getVehicleType());

        // Scale the limit by the ridden entity's MOVEMENT_SPEED attribute ratio.
        // Plugin-spawned fast horses (or pigs/striders with a boosted attribute) will have
        // their actual attribute value tracked server-side; we scale accordingly so legitimate
        // fast mounts are never flagged.
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

        boolean speedFlag = speed > limit;

        if (!speedFlag) {
            buffer = Math.max(0, buffer - 0.02);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "EntitySpeed");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 1.5 : 0.5;
        if (buffer > 3.0) {
            flagAndAlert(String.format("speed=%.2f limit=%.2f netty=%.1f/s spartan=%s",
                speed, limit, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
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
     * entity type. These match the defaults set in the corresponding PacketEntity constructors.
     * Returns 0 for vehicles that do not track MOVEMENT_SPEED (boats, minecarts) — callers must
     * guard against a zero return value.
     */
    private double getBaseSpeedForVehicle(EntityType type) {
        if (type == null) return 0;
        if (type == EntityTypes.PIG) return 0.1;        // PacketEntityRideable default
        if (type == EntityTypes.STRIDER) return 0.1;    // PacketEntityStrider → PacketEntityRideable default
        if (EntityTypes.isTypeInstanceOf(type, EntityTypes.CHESTED_HORSE)) return 0.175; // Donkey/mule
        if (type == EntityTypes.CAMEL) return 0.09;
        if (type == EntityTypes.ZOMBIE_HORSE || type == EntityTypes.SKELETON_HORSE) return 0.2;
        if (EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_HORSE)) return 0.225; // Regular horse / llama etc.
        // Boats and minecarts do not have a MOVEMENT_SPEED attribute — return 0 to skip scaling.
        return 0;
    }
}
