package dev.yanianz.sourbyanticheat.checks.type;

import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

public interface PostPredictionCheck extends PacketCheck {

    default void onPredictionComplete(final PredictionComplete predictionComplete) {
    }
}
