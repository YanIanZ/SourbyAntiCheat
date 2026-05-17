package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;

@CheckData(name = "CrossJump", configName = "crossjump", decay = 0.02, setback = 10, stableKey = "cross.jump")
public class CrossJump extends Check implements PostPredictionCheck {

    private double buffer;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double jumpThreshold       = 1.25;
    private static final double NETTY_DELAY_THRESHOLD = 40.0; // physics constant, stays static

    public CrossJump(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        jumpThreshold = config.getDoubleElse(base + "jump-threshold", 1.25);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.compensatedEntities.self.isDead) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        // Exempt: elytra gliding, flight, Jump Boost (handled via velocity scaling below), levitation, Slow Falling
        if (player.inVehicle() || player.canFly || player.isGliding
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.LEVITATION)
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.SLOW_FALLING)) return;

        double deltaY = player.crossValidationData.pePositionDeltaY;

        // Scale the clientVelocity.getY() cutoff with Jump Boost amplifier.
        // Vanilla Jump Boost adds ~0.1 per level to the jump impulse (base ~0.42).
        // We use getPotionEffectLevel which returns the amplifier (0-based: JB I = 0, JB II = 1, ...).
        // Base cutoff 0.6; each Jump Boost level adds 0.1 (JB I → 0.7, JB II → 0.8, etc.)
        double velYCutoff = 0.6;
        var jumpBoostLevel = player.compensatedEntities.self.getPotionEffectLevel(PotionTypes.JUMP_BOOST);
        if (jumpBoostLevel.isPresent()) {
            // amplifier is 0-based: level I = amplifier 0, level II = amplifier 1, …
            velYCutoff += (jumpBoostLevel.getAsInt() + 1) * 0.1;
        }

        boolean highJump = deltaY > jumpThreshold && player.clientVelocity.getY() <= velYCutoff;

        if (!highJump) {
            buffer = Math.max(0, buffer - 0.02);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyAvgDelayBetweenPacketsMs < NETTY_DELAY_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Step");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        int ping = player.getTransactionPing();
        double multiplier = ping > 400 ? 0.5 : 1.0;
        buffer += ((nettyConfirms || spartanConfirms) ? 1.5 : 0.5) * multiplier;
        if (buffer > 3.0) {
            flagAndAlert(String.format("dY=%.3f velY=%.3f cut=%.2f netty=%.1fms spartan=%s",
                deltaY, player.clientVelocity.getY(), velYCutoff,
                player.crossValidationData.nettyAvgDelayBetweenPacketsMs, spartanResult.type()));
            return;
        }
        // reward() only on confirmed-clean paths above
    }
}
