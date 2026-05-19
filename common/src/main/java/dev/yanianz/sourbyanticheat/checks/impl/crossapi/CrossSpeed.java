package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import dev.yanianz.sourbyanticheat.utils.viaversion.ViaVersionUtil;

@CheckData(name = "CrossSpeed", configName = "crossspeed", decay = 0.05, setback = 25, stableKey = "cross.speed")
public class CrossSpeed extends Check implements PostPredictionCheck {

    private double buffer;

    // Config-wired: the prediction offset (blocks the actual movement deviates from
    // what Grim's engine predicts) above which a tick is suspicious.
    private double offsetThreshold = 0.15;

    private static final double CROSS_VERSION_LENIENCY = 1.5;
    private static final double VIA_BACKWARDS_SPRINT_LENIENCY = 1.3;

    public CrossSpeed(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        offsetThreshold = config.getDoubleElse(getConfigName() + ".offset-threshold", 0.15);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.inVehicle() || player.compensatedEntities.self.isDead
                || player.packetStateData.lastPacketWasTeleport) return;

        // Detection is the prediction OFFSET only — Grim's authoritative measure of how
        // far the player's actual movement deviated from physically possible movement.
        // The previous actualH/velH heuristic compared clientVelocity against
        // pePositionDelta, which are NOT tick-synchronised — it was noisy and
        // false-flagged legitimate players, which is the false positive this fixes.
        double offset = player.crossValidationData.offsetFromPrediction;

        double effectiveOffsetThreshold = offsetThreshold;
        if (ViaVersionUtil.isCrossVersion(player)) {
            effectiveOffsetThreshold *= CROSS_VERSION_LENIENCY;
        }
        if (ViaVersionUtil.isViaBackwardsPre1_9(player)) {
            effectiveOffsetThreshold *= VIA_BACKWARDS_SPRINT_LENIENCY;
        }

        if (offset <= effectiveOffsetThreshold) {
            buffer = Math.max(0, buffer - 0.05);
            reward();
            return;
        }

        int ping = player.getTransactionPing();
        double pingMultiplier = ping > 400 ? 0.5 : 1.0;
        buffer += 1.0 * pingMultiplier;

        if (buffer > 4.0) {
            // Only alert when Spartan independently confirms Speed; otherwise decay
            // and reward so a borderline-offset legitimate player is not false-banned.
            SpartanCrossCheck.CrossCheckResult spartanResult =
                SpartanCrossCheck.checkSpartan(player.uuid, "Speed");
            if (spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED) {
                flagAndAlert(String.format("offset=%.3f buffer=%.1f netty=%.1f/s spartan=%s",
                    offset, buffer,
                    player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
                return;
            }
            buffer = Math.max(0, buffer - 0.5);
        }
        reward();
    }
}
