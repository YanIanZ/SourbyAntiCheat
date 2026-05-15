package dev.yanianz.sourbyanticheat.predictionengine.predictions.rideable;

import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.predictionengine.predictions.PredictionEngineWaterLegacy;
import dev.yanianz.sourbyanticheat.utils.data.VectorData;
import dev.yanianz.sourbyanticheat.utils.math.Vector3dm;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class PredictionEngineRideableWaterLegacy extends PredictionEngineWaterLegacy {
    private final Vector3dm movementVector;

    @Override
    public void addJumpsToPossibilities(SacPlayer player, Set<VectorData> existingVelocities) {
        PredictionEngineRideableUtils.handleJumps(player, existingVelocities);
    }

    @Override
    public List<VectorData> applyInputsToVelocityPossibilities(SacPlayer player, Set<VectorData> possibleVectors, float speed) {
        return PredictionEngineRideableUtils.applyInputsToVelocityPossibilities(movementVector, player, possibleVectors, speed);
    }
}
