package dev.yanianz.sourbyanticheat.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.VehiclePositionUpdate;

public interface VehicleCheck extends AbstractCheck {

    void process(final VehiclePositionUpdate vehicleUpdate);
}
