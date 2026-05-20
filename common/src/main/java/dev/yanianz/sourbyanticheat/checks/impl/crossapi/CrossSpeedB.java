package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "CrossSpeedB", configName = "crossspeedb", decay = 0.01, setback = 10, stableKey = "cross.speed_b")
public class CrossSpeedB extends Check implements PacketCheck {

    private double buffer;
    private double consistentRatio;
    private int ticksSinceLastFlag;

    private double maxRatioDeviation = 0.5;
    private double ratioThreshold    = 1.8;
    private double sprintRatioCap    = 2.4;
    private double velFloor          = 0.01;
    private double nettyRateThreshold = 120.0;
    private static final double BUFFER_CAP = 5.0;

    public CrossSpeedB(SacPlayer player) { super(player); }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        maxRatioDeviation  = config.getDoubleElse(base + "max-ratio-deviation", 0.5);
        ratioThreshold     = config.getDoubleElse(base + "ratio-threshold", 1.8);
        sprintRatioCap     = config.getDoubleElse(base + "sprint-ratio-cap", 2.4);
        velFloor           = config.getDoubleElse(base + "vel-floor", 0.01);
        nettyRateThreshold = config.getDoubleElse(base + "netty-rate-threshold", 120.0);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport || player.inVehicle()
                || player.canFly || player.isGliding
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR
                || player.compensatedEntities.self.isDead) return;

        double velX = player.clientVelocity.getX();
        double velZ = player.clientVelocity.getZ();
        double velH = Math.sqrt(velX * velX + velZ * velZ);
        if (velH < velFloor) return;

        double actualX = player.crossValidationData.pePositionDeltaX;
        double actualZ = player.crossValidationData.pePositionDeltaZ;
        double actualH = Math.sqrt(actualX * actualX + actualZ * actualZ);
        if (actualH < velFloor) return;

        double ratio = actualH / velH;
        double diff = Math.abs(ratio - consistentRatio);

        double effectiveThreshold = player.isSprinting ? sprintRatioCap : ratioThreshold;

        if (diff < maxRatioDeviation && ratio > effectiveThreshold) {
            buffer = Math.min(BUFFER_CAP, buffer + 0.5);
        } else {
            buffer = Math.max(0, buffer - 1.0);
            consistentRatio = ratio;
        }

        if (buffer < 3.0) {
            ticksSinceLastFlag = 0;
            reward();
            return;
        }

        ticksSinceLastFlag++;
        if (ticksSinceLastFlag < 10) {
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > nettyRateThreshold;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "Speed");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (spartanConfirms && nettyConfirms) {
            flagAndAlert(String.format("ratio=%.2f buffer=%.1f netty=%.1f/s sprint=%b spartan=%s",
                ratio, buffer, player.crossValidationData.nettyPacketRatePerSec, player.isSprinting, spartanResult.type()));
            buffer = 0;
            ticksSinceLastFlag = 0;
        } else {
            buffer = Math.max(0, buffer - 1.0);
            reward();
        }
    }
}
