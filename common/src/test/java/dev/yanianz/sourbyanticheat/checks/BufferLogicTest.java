package dev.yanianz.sourbyanticheat.checks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BufferLogicTest {

    @Test
    void bufferAccumulatesWithCap() {
        double buffer = 0;
        double cap = 5.0;
        for (int i = 0; i < 20; i++) {
            buffer = Math.min(cap, buffer + 0.5);
        }
        assertEquals(5.0, buffer, 0.001);
    }

    @Test
    void bufferDecaysToZero() {
        double buffer = 3.0;
        for (int i = 0; i < 10; i++) {
            buffer = Math.max(0, buffer - 0.5);
        }
        assertEquals(0.0, buffer, 0.001);
    }

    @Test
    void bufferNeverNegative() {
        double buffer = 0.1;
        buffer = Math.max(0, buffer - 1.0);
        assertEquals(0.0, buffer, 0.001);
    }

    @Test
    void twoPhaseConfirmPattern() {
        // Simulates: +2 if both confirm, +1 if one confirms, decay if none
        int buffer = 0;
        int cap = 5;

        // Neither confirms -> decay
        buffer = Math.max(0, buffer - 1);
        assertEquals(0, buffer);

        // One confirms
        buffer = Math.min(cap, buffer + 1);
        assertEquals(1, buffer);

        // Both confirm
        buffer = Math.min(cap, buffer + 2);
        assertEquals(3, buffer);

        // Both confirm again
        buffer = Math.min(cap, buffer + 2);
        assertEquals(5, buffer);

        // Cap prevents overflow
        buffer = Math.min(cap, buffer + 2);
        assertEquals(5, buffer);
    }

    @Test
    void sprintAwareThreshold() {
        double ratio = 1.83;
        double nonSprintThreshold = 1.8;
        double sprintThreshold = 2.4;

        assertTrue(ratio > nonSprintThreshold);
        assertFalse(ratio > sprintThreshold);
    }

    @Test
    void minPredictedMovementFloor() {
        double predicted = 0.075;
        double floor = 0.15;
        assertFalse(predicted >= floor);

        predicted = 0.5;
        assertTrue(predicted >= floor);
    }

    @Test
    void nettyVarianceCheck() {
        double variance = 22.0;
        double threshold = 12.0;
        // High variance means NOT bot-like — legit player
        assertFalse(variance < threshold);

        variance = 8.0;
        // Low variance means bot-like — suspicious
        assertTrue(variance < threshold);
    }
}
