package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossFlightB", configName = "crossflightb", decay = 0.02, setback = 10, stableKey = "cross.flight_b")
public class CrossFlightB extends Check implements PostPredictionCheck {

    private double buffer;
    private int hoverTicks;
    private static final double NETTY_RATE_THRESHOLD = 18.0;

    public CrossFlightB(SacPlayer player) { super(player); }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.compensatedEntities.self.isDead) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.compensatedEntities.self.hasPotionEffect(PotionTypes.LEVITATION)
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.SLOW_FALLING)
                || player.inVehicle() || player.isGliding || player.canFly || player.wasTouchingWater) return;

        double deltaY = player.crossValidationData.pePositionDeltaY;
        boolean onGround = player.crossValidationData.peOnGround;

        if (!onGround && Math.abs(deltaY) < 0.01) {
            hoverTicks++;
        } else {
            hoverTicks = Math.max(0, hoverTicks - 2);
            buffer = Math.max(0, buffer - 0.02);
            // reward() every clean tick — not gated by buffer < 0.01
            reward();
            return;
        }

        if (hoverTicks < 10) return;

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "Flight");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        int ping = player.getTransactionPing();
        double multiplier = ping > 400 ? 0.5 : 1.0;
        buffer += ((nettyConfirms || spartanConfirms) ? 1.5 : 0.5) * multiplier;
        if (buffer > 3.0) {
            flagAndAlert(String.format("hover=%d netty=%.1f/s spartan=%s",
                hoverTicks, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
