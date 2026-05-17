package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossEntitySpeed", configName = "crossentityspeed", decay = 0.02, setback = 10, stableKey = "cross.entityspeed")
public class CrossEntitySpeed extends Check implements PostPredictionCheck {

    private double buffer;
    private static final double NETTY_RATE_THRESHOLD = 15.0;

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
}
