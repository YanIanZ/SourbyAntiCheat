package dev.yanianz.sourbyanticheat.checks.type;

import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockBreak;

public interface BlockBreakCheck extends PostPredictionCheck {
    default void onBlockBreak(final BlockBreak blockBreak) {
    }

    default void onPostFlyingBlockBreak(final BlockBreak blockBreak) {
    }
}
