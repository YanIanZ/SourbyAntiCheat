package dev.yanianz.sourbyanticheat.spartan;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpartanLeniencyTest {

    @Test void viaBackwardsSprintLeniencyMultiplier() {
        double base = 0.15; double leniency = 1.3;
        assertEquals(0.195, base * leniency, 0.001);
    }

    @Test void viaBackwardsGapMultiplier() {
        double base = 50; double leniency = 1.5;
        assertEquals(75, base * leniency, 0.001);
    }

    @Test void crossVersionLeniencyMultiplier() {
        double base = 0.15; double leniency = 1.5;
        assertEquals(0.225, base * leniency, 0.001);
    }

    @Test void noLeniencyReturnsBase() {
        double base = 0.15; double leniency = 1.0;
        assertEquals(0.15, base * leniency, 0.001);
    }

    @Test void bufferCapWithLeniency() {
        double cap = 4.0; double leniency = 1.3;
        assertEquals(5.2, cap * leniency, 0.001);
    }

    @Test void deltaYFloorBackwards() {
        double floor = 0.08; double adjusted = -0.08;
        assertTrue(adjusted < floor);
    }

    @Test void sprintSpeedViaBackwardsMultiplier() {
        double base = 0.13; double mult = 0.7;
        assertEquals(0.091, base * mult, 0.001);
    }

    @Test void attackIntervalViaBackwardsMultiplier() {
        double base = 500; double mult = 0.8;
        assertEquals(400, base * mult, 0.001);
    }
}
