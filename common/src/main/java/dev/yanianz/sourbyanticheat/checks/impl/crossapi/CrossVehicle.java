package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import dev.yanianz.sourbyanticheat.utils.data.packetentity.PacketEntity;

@CheckData(name = "CrossVehicle", configName = "crossvehicle", decay = 0.02, setback = 10, stableKey = "cross.vehicle")
public class CrossVehicle extends Check implements PostPredictionCheck {

    private double buffer;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double maxHorseSpeed       = 0.45;
    private double maxBoatSpeed        = 0.40;
    private double nettyRateThreshold  = 15.0;

    public CrossVehicle(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        maxHorseSpeed      = config.getDoubleElse(base + "max-horse-speed",      0.45);
        maxBoatSpeed       = config.getDoubleElse(base + "max-boat-speed",       0.40);
        nettyRateThreshold = config.getDoubleElse(base + "netty-rate-threshold", 15.0);
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

        double deltaX = player.x - player.lastX;
        double deltaZ = player.z - player.lastZ;
        double speed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        PacketEntity vehicle = player.getVehicle();
        boolean isBoat = vehicle != null && EntityTypes.isTypeInstanceOf(vehicle.type, EntityTypes.BOAT);
        double speedLimit = isBoat ? maxBoatSpeed : maxHorseSpeed;

        boolean speedFlag = speed > speedLimit;

        if (!speedFlag) {
            buffer = Math.max(0, buffer - 0.02);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > nettyRateThreshold;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "BoatMove");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 1.5 : 0.5;
        if (buffer > 3.0) {
            flagAndAlert(String.format("speed=%.2f limit=%.2f boat=%s netty=%.1f/s spartan=%s",
                speed, speedLimit, isBoat, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
