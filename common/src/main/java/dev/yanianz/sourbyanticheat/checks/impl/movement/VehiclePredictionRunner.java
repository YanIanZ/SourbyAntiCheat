package dev.yanianz.sourbyanticheat.checks.impl.movement;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.type.VehicleCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PositionUpdate;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.VehiclePositionUpdate;

public class VehiclePredictionRunner extends Check implements VehicleCheck {
    public VehiclePredictionRunner(SacPlayer playerData) {
        super(playerData);
    }

    @Override
    public void process(final VehiclePositionUpdate vehicleUpdate) {
        // Vehicle onGround = false always
        // We don't do vehicle setbacks because vehicle netcode sucks.
        player.movementCheckRunner.processAndCheckMovementPacket(new PositionUpdate(vehicleUpdate.from(), vehicleUpdate.to(), false, null, null, vehicleUpdate.isTeleport()));
    }
}
