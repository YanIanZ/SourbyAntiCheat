package dev.yanianz.sourbyanticheat.predictionengine.predictions.rideable;

import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.predictionengine.predictions.PredictionEngineWater;
import dev.yanianz.sourbyanticheat.utils.data.VectorData;
import dev.yanianz.sourbyanticheat.utils.math.SacMath;
import dev.yanianz.sourbyanticheat.utils.math.Vector3dm;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class PredictionEngineRideableWater extends PredictionEngineWater {
    protected final Vector3dm movementVector;

    @Override
    public void addJumpsToPossibilities(SacPlayer player, Set<VectorData> existingVelocities) {
        PredictionEngineRideableUtils.handleJumps(player, existingVelocities);
    }

    @Override
    public List<VectorData> applyInputsToVelocityPossibilities(SacPlayer player, Set<VectorData> possibleVectors, float speed) {
        return PredictionEngineRideableUtils.applyInputsToVelocityPossibilities(this, movementVector, player, possibleVectors, speed);
    }

    @Override
    public Vector3dm getMovementResultFromInput(SacPlayer player, Vector3dm inputVector, float flyingSpeed, float yRot) {
        float yRotRadians = SacMath.radians(yRot);
        float sin = player.trigHandler.sin(yRotRadians);
        float cos = player.trigHandler.cos(yRotRadians);

        double xResult = inputVector.getX() * cos - inputVector.getZ() * sin;
        double zResult = inputVector.getZ() * cos + inputVector.getX() * sin;

        return new Vector3dm(xResult * flyingSpeed, inputVector.getY() * flyingSpeed, zResult * flyingSpeed);
    }

}
