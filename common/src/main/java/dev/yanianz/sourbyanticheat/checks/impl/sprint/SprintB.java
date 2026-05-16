package dev.yanianz.sourbyanticheat.checks.impl.sprint;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import dev.yanianz.sourbyanticheat.utils.enums.Pose;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

import java.util.Collections;

@CheckData(name = "SprintB", stableKey = "sac.sprint.sneaking", description = "Sprinting while sneaking or crawling", setback = 5)
public class SprintB extends Check implements PostPredictionCheck {
    public SprintB(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (player.isSlowMovement && player.sneakingSpeedMultiplier < 0.8f && predictionComplete.isChecked()) {
            ClientVersion version = player.getClientVersion();

            // https://bugs.mojang.com/browse/MC-152728
            if (version.isNewerThanOrEquals(ClientVersion.V_1_14_2) && version != ClientVersion.V_1_21_4) {
                return;
            }

            // https://github.com/GrimAnticheat/Grim/issues/1932
            if (version.isNewerThanOrEquals(ClientVersion.V_1_14) && player.wasFlying && player.lastPose == Pose.FALL_FLYING && !player.isGliding) {
                return;
            }

            // https://github.com/GrimAnticheat/Grim/issues/1948
            if (version == ClientVersion.V_1_21_4 && (Collections.max(player.uncertaintyHandler.pistonX) != 0
                    || Collections.max(player.uncertaintyHandler.pistonY) != 0
                    || Collections.max(player.uncertaintyHandler.pistonZ) != 0)) {
                return;
            }

            if (!player.wasTouchingWater || version.isOlderThan(ClientVersion.V_1_13)) {
                return;
            }

            if (player.isSprinting) {
                flagAndAlertWithSetback();
            } else reward();
        }
    }
}
