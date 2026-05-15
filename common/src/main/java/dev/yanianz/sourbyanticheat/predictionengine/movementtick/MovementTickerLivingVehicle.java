package dev.yanianz.sourbyanticheat.predictionengine.movementtick;

import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.predictionengine.predictions.rideable.PredictionEngineRideableLava;
import dev.yanianz.sourbyanticheat.predictionengine.predictions.rideable.PredictionEngineRideableNormal;
import dev.yanianz.sourbyanticheat.predictionengine.predictions.rideable.PredictionEngineRideableWater;
import dev.yanianz.sourbyanticheat.predictionengine.predictions.rideable.PredictionEngineRideableWaterLegacy;
import dev.yanianz.sourbyanticheat.utils.math.Vector3dm;
import dev.yanianz.sourbyanticheat.utils.nmsutil.BlockProperties;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

public class MovementTickerLivingVehicle extends MovementTicker {
    protected Vector3dm movementInput = new Vector3dm();

    public MovementTickerLivingVehicle(SacPlayer player) {
        super(player);
    }

    @Override
    public void doWaterMove(float swimSpeed, boolean isFalling, float swimFriction) {
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13)) {
            new PredictionEngineRideableWater(movementInput).guessBestMovement(swimSpeed, player, isFalling, player.gravity, swimFriction);
        } else {
            new PredictionEngineRideableWaterLegacy(movementInput).guessBestMovement(swimSpeed, player, swimFriction);
        }
    }

    @Override
    public void doLavaMove() {
        new PredictionEngineRideableLava(movementInput).guessBestMovement(0.02F, player);
    }

    @Override
    public void doNormalMove(float blockFriction) {
        new PredictionEngineRideableNormal(movementInput).guessBestMovement(BlockProperties.getFrictionInfluencedSpeed(blockFriction, player), player);
    }
}
