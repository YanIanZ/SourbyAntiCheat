package dev.yanianz.sourbyanticheat.predictionengine.predictions;

import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.data.VectorData;
import dev.yanianz.sourbyanticheat.utils.enums.FluidTag;
import dev.yanianz.sourbyanticheat.utils.math.SacMath;
import dev.yanianz.sourbyanticheat.utils.math.Vector3dm;

import java.util.HashSet;
import java.util.Set;

public class PredictionEngineLava extends PredictionEngine {
    @Override
    public void addJumpsToPossibilities(SacPlayer player, Set<VectorData> existingVelocities) {
        for (VectorData vector : new HashSet<>(existingVelocities)) {
            if (player.couldSkipTick && vector.isZeroPointZeroThree()) {
                double extraVelFromVertTickSkipUpwards = SacMath.clamp(player.actualMovement.getY(), vector.vector.clone().getY(), vector.vector.clone().getY() + 0.05f);
                existingVelocities.add(new VectorData(vector.vector.clone().setY(extraVelFromVertTickSkipUpwards), vector, VectorData.VectorType.Jump));
            } else {
                existingVelocities.add(new VectorData(vector.vector.clone().add(0, 0.04f, 0), vector, VectorData.VectorType.Jump));
            }

            if (player.getFluidHeight(FluidTag.LAVA) <= 0.4D && player.lastOnGround && !player.onGround) {
                Vector3dm withJump = vector.vector.clone();
                super.doJump(player, withJump);
                existingVelocities.add(new VectorData(withJump, vector, VectorData.VectorType.Jump));
            }
        }
    }
}
