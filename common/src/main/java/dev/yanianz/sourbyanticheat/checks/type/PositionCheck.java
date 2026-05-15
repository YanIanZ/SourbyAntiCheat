package dev.yanianz.sourbyanticheat.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PositionUpdate;

public interface PositionCheck extends AbstractCheck {

    default void onPositionUpdate(final PositionUpdate positionUpdate) {
    }
}
