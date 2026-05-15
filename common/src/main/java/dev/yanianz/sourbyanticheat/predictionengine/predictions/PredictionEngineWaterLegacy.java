package dev.yanianz.sourbyanticheat.predictionengine.predictions;

import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.data.VectorData;
import dev.yanianz.sourbyanticheat.utils.math.SacMath;
import dev.yanianz.sourbyanticheat.utils.math.Vector3dm;

import java.util.HashSet;
import java.util.Set;

public class PredictionEngineWaterLegacy extends PredictionEngine {
    private float swimmingSpeed;
    private float swimmingFriction;

    public void guessBestMovement(float swimmingSpeed, SacPlayer player, float swimmingFriction) {
        this.swimmingSpeed = swimmingSpeed;
        this.swimmingFriction = swimmingFriction;
        super.guessBestMovement(swimmingSpeed, player);
    }

    // This is just the vanilla equation for legacy water movement
    @Override
    public Vector3dm getMovementResultFromInput(SacPlayer player, Vector3dm inputVector, float f, float f2) {
        float lengthSquared = (float) inputVector.lengthSquared();

        if (lengthSquared >= 1.0E-4F) {
            lengthSquared = (float) Math.sqrt(lengthSquared);

            if (lengthSquared < 1.0F) {
                lengthSquared = 1.0F;
            }

            lengthSquared = swimmingSpeed / lengthSquared;
            inputVector.multiply(lengthSquared);
            float yawRadians = SacMath.radians(player.yaw);
            float sinResult = player.trigHandler.sin(yawRadians);
            float cosResult = player.trigHandler.cos(yawRadians);

            return new Vector3dm(inputVector.getX() * cosResult - inputVector.getZ() * sinResult,
                    inputVector.getY(), inputVector.getZ() * cosResult + inputVector.getX() * sinResult);
        }

        return new Vector3dm();
    }


    @Override
    public void addJumpsToPossibilities(SacPlayer player, Set<VectorData> existingVelocities) {
        for (VectorData vector : new HashSet<>(existingVelocities)) {
            existingVelocities.add(new VectorData(vector.vector.clone().add(0, 0.04f, 0), vector, VectorData.VectorType.Jump));

            if (player.skippedTickInActualMovement) {
                existingVelocities.add(new VectorData(vector.vector.clone().add(0, 0.02f, 0), vector, VectorData.VectorType.Jump));
            }
        }
    }

    @Override
    public void endOfTick(SacPlayer player, double playerGravity) {
        super.endOfTick(player, playerGravity);

        for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
            vector.vector.multiply(swimmingFriction, 0.8F, swimmingFriction);

            // Gravity
            vector.vector.setY(vector.vector.getY() - 0.02D);
        }
    }
}
