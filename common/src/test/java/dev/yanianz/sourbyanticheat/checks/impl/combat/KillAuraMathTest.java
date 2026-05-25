package dev.yanianz.sourbyanticheat.checks.impl.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KillAuraMathTest {

    @Test
    void lookingDirectlyAtTargetIsZero() {
        assertEquals(0.0, KillAuraMath.angleDegrees(0, 0, 1, 0, 0, 5), 1e-6);
    }

    @Test
    void perpendicularIs90() {
        assertEquals(90.0, KillAuraMath.angleDegrees(0, 0, 1, 5, 0, 0), 1e-6);
    }

    @Test
    void oppositeIs180() {
        assertEquals(180.0, KillAuraMath.angleDegrees(0, 0, 1, 0, 0, -5), 1e-6);
    }

    @Test
    void fortyFiveDegrees() {
        assertEquals(45.0, KillAuraMath.angleDegrees(0, 0, 1, 5, 0, 5), 1e-6);
    }

    @Test
    void degenerateVectorsReturnZero() {
        assertEquals(0.0, KillAuraMath.angleDegrees(0, 0, 0, 1, 2, 3), 1e-9);
        assertEquals(0.0, KillAuraMath.angleDegrees(1, 2, 3, 0, 0, 0), 1e-9);
    }

    @Test
    void clampsAgainstFloatingPointOverflow() {
        // identical normalized dirs must not NaN from acos(>1)
        double a = KillAuraMath.angleDegrees(0.5, 0.5, 0.5, 0.5, 0.5, 0.5);
        assertFalse(Double.isNaN(a));
        assertEquals(0.0, a, 1e-6);
    }
}
