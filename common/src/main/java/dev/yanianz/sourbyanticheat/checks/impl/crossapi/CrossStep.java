package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;

@CheckData(name = "CrossStep", configName = "crossstep", decay = 0.1, setback = 10, stableKey = "cross.step")
public class CrossStep extends Check implements PacketCheck {

    private double stepBuffer;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double stepThreshold       = 0.6;
    private double nettyDelayThreshold = 40.0;
    private double nettyRateThreshold  = 120.0;

    public CrossStep(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        stepThreshold       = config.getDoubleElse(base + "step-threshold", 0.6);
        nettyDelayThreshold = config.getDoubleElse(base + "netty-delay-threshold", 40.0);
        nettyRateThreshold  = config.getDoubleElse(base + "netty-rate-threshold", 120.0);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.compensatedEntities.self.hasPotionEffect(PotionTypes.JUMP_BOOST)
            || player.compensatedEntities.self.hasPotionEffect(PotionTypes.LEVITATION)) return;
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.inVehicle() || player.compensatedEntities.self.isDead) return;

        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        if (player.packetStateData.lastPacketWasTeleport) {
            stepBuffer = 0;
            return;
        }

        double deltaY = player.crossValidationData.pePositionDeltaY;
        boolean stepSpike = deltaY > stepThreshold;
        boolean notJumping = player.clientVelocity.getY() <= 0;

        if (!stepSpike || !notJumping) {
            stepBuffer = Math.max(0, stepBuffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyAvgDelayBetweenPacketsMs < nettyDelayThreshold
            && player.crossValidationData.nettyPacketRatePerSec > nettyRateThreshold;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Step");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        int ping = player.getTransactionPing();
        double multiplier = ping > 400 ? 0.5 : 1.0;

        if (nettyConfirms || spartanConfirms) {
            stepBuffer += 2 * multiplier;
        } else {
            stepBuffer += multiplier;
        }

        if (stepBuffer > 3) {
            flagAndAlert(String.format("dy=%.3f netty=%.1f/s delay=%.1fms spartan=%s",
                deltaY, player.crossValidationData.nettyPacketRatePerSec,
                player.crossValidationData.nettyAvgDelayBetweenPacketsMs, spartanResult.type()));
        }
    }
}
