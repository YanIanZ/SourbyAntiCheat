package dev.yanianz.sourbyanticheat.checks.impl.movement;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

@CheckData(name = "NoSlow", stableKey = "sac.movement.noslow", description = "Was not slowed while using an item", setback = 5)
public class NoSlow extends Check implements PostPredictionCheck {
    // The player sends that they switched items the next tick if they switch from an item that can be used
    // to another item that can be used.  What the fuck mojang.  Affects 1.8 (and most likely 1.7) clients.
    // Public + set externally by the packet listener when the player's held hotbar slot
    // changes — NoSlow reads it to apply the 1.8 first-tick-not-slowed quirk below.
    public boolean didSlotChangeLastTick = false;
    public boolean flaggedLastTick = false;
    // Default raised from 0.001: a 0.001 offset false-flags high-latency players whose
    // predicted velocity drifts slightly from the (uncertain) item-use slowdown. 0.01 is
    // still well below any real NoSlow bypass, which removes the full slowdown factor.
    private double offsetToFlag = 0.01;
    private double bestOffset = 1;

    public NoSlow(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked()) return;

        // If the player was using an item for certain, and their predicted velocity had a flipped item
        if (player.packetStateData.isSlowedByUsingItem()) {
            // 1.8 users are not slowed the first tick they use an item, strangely
            if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8) && didSlotChangeLastTick) {
                didSlotChangeLastTick = false;
                flaggedLastTick = false;
            }

            if (bestOffset > offsetToFlag) {
                if (flaggedLastTick) {
                    flagAndAlertWithSetback();
                }
                flaggedLastTick = true;
            } else {
                reward();
                flaggedLastTick = false;
            }
        } else {
            // Not using an item this tick — clear stale state so a leftover true cannot
            // combine with a future using-item tick into a false flag.
            flaggedLastTick = false;
        }
        bestOffset = 1;
    }

    public void handlePredictionAnalysis(double offset) {
        bestOffset = Math.min(bestOffset, offset);
    }

    @Override
    public void onReload(ConfigManager config) {
        offsetToFlag = config.getDoubleElse(getConfigName() + ".offset-to-flag", 0.01);
    }
}
