package dev.yanianz.sourbyanticheat.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.RotationUpdate;

public interface RotationCheck extends AbstractCheck {

    default void process(final RotationUpdate rotationUpdate) {
    }
}
