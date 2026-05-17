package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

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

    private int stepBuffer;
    private static final double STEP_THRESHOLD = 0.6;
    private static final double NETTY_DELAY_THRESHOLD = 40.0;
    private static final double NETTY_RATE_THRESHOLD = 18.0;

    public CrossStep(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.compensatedEntities.self.hasPotionEffect(PotionTypes.JUMP_BOOST)
            || player.compensatedEntities.self.hasPotionEffect(PotionTypes.LEVITATION)) return;
        if (player.disableGrim) return;

        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        if (player.packetStateData.lastPacketWasTeleport) {
            stepBuffer = 0;
            return;
        }

        double deltaY = player.crossValidationData.pePositionDeltaY;
        boolean stepSpike = deltaY > STEP_THRESHOLD;
        boolean notJumping = player.clientVelocity.getY() <= 0;

        if (!stepSpike || !notJumping) {
            stepBuffer = Math.max(0, stepBuffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyAvgDelayBetweenPacketsMs < NETTY_DELAY_THRESHOLD
            && player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Step");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            stepBuffer += 2;
        } else {
            stepBuffer += 1;
        }

        if (stepBuffer > 3) {
            flagAndAlertWithSetback(String.format("dy=%.3f netty=%.1f/s delay=%.1fms spartan=%s",
                deltaY, player.crossValidationData.nettyPacketRatePerSec,
                player.crossValidationData.nettyAvgDelayBetweenPacketsMs, spartanResult.type()));
        }
    }
}
