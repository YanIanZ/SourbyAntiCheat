package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "CrossElytraMove", configName = "crosselytramove", decay = 0.05, setback = 12, stableKey = "cross.elytramove")
public class CrossElytraMove extends Check implements PacketCheck {

    private int elytraBuffer;
    private double speedThreshold = 30.0;
    private static final double NETTY_RATE_THRESHOLD = 18.0;

    public CrossElytraMove(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        this.speedThreshold = config.getDoubleElse(getConfigName() + ".speed-threshold", 30.0);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.compensatedEntities.self.isDead) return;
        if (player.packetStateData.lastPacketWasTeleport) return;

        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        boolean gliding = player.crossValidationData.peGliding;
        if (!gliding) {
            elytraBuffer = Math.max(0, elytraBuffer - 1);
            reward();
            return;
        }

        // Slow-Falling / Levitation exemption — these effects can cause unusual elytra speeds
        if (player.compensatedEntities.self.hasPotionEffect(PotionTypes.SLOW_FALLING)
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.LEVITATION)) {
            reward();
            return;
        }

        double dx = player.crossValidationData.pePositionDeltaX;
        double dy = player.crossValidationData.pePositionDeltaY;
        double dz = player.crossValidationData.pePositionDeltaZ;
        double speed = Math.sqrt(dx * dx + dy * dy + dz * dz);

        boolean speedFlag = speed > speedThreshold;

        if (!speedFlag) {
            elytraBuffer = Math.max(0, elytraBuffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "ElytraMove");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            elytraBuffer += 2;
        } else {
            elytraBuffer += 1;
        }

        if (elytraBuffer > 3) {
            flagAndAlert(String.format("speed=%.1f netty=%.1f/s spartan=%s",
                speed, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
