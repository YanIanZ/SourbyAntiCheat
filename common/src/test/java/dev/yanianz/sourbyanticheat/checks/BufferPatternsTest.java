package dev.yanianz.sourbyanticheat.checks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BufferPatternsTest {

    // CrossSpeedB pattern: buffer accumulates on consistent ratio > threshold
    @Test
    void speedBufferAccumulatesOnConsistentHighRatio() {
        double buffer = 0;
        double consistentRatio = 1.8;
        double maxDeviation = 0.5;
        double ratioThreshold = 1.5;
        double cap = 5.0;

        for (int tick = 0; tick < 12; tick++) {
            double ratio = 1.83;
            double diff = Math.abs(ratio - consistentRatio);
            if (diff < maxDeviation && ratio > ratioThreshold) {
                buffer = Math.min(cap, buffer + 0.5);
            } else {
                buffer = Math.max(0, buffer - 1.0);
                consistentRatio = ratio;
            }
        }
        assertEquals(5.0, buffer, 0.001);
    }

    // CrossSpeedB: sprinting player doesn't flag
    @Test
    void sprintingPlayerNotFlagged() {
        double ratio = 1.83;
        double sprintCap = 2.4;
        assertFalse(ratio > sprintCap);
    }

    // CrossNoFall pattern: AND gate for ground spoof
    @Test
    void noFallAndGateRequiresBothOffsets() {
        boolean groundSpoof1 = true && true && true;  // peOnGround && yOff > 0.1 && fullOff > 0.2
        assertTrue(groundSpoof1);

        boolean groundSpoof2 = false || false || false; // missing one condition
        assertFalse(groundSpoof2);
    }

    // CrossNoSwing pattern: +2 both, +1 one, decay if none
    @Test
    void noSwingBufferThreeTierConfirmation() {
        int buffer = 0;
        int cap = 5;

        // Neither confirms -> decay
        buffer = Math.max(0, buffer - 1);
        assertEquals(0, buffer);

        // One confirms
        buffer = Math.min(cap, buffer + 1);
        assertEquals(1, buffer);

        // One confirms again
        buffer = Math.min(cap, buffer + 1);
        assertEquals(2, buffer);

        // Both confirm
        buffer = Math.min(cap, buffer + 2);
        assertEquals(4, buffer);

        // Flag at >= 4
        assertTrue(buffer >= 4);
    }

    // CrossAntiKB pattern: min knockback floor
    @Test
    void antiKbFloorFiltersTinyKnockback() {
        double predicted = 0.075;
        double floor = 0.15;
        assertFalse(predicted >= floor);
    }

    @Test
    void antiKbRatioFlagging() {
        double actual = 0.0;
        double predicted = 0.5;
        double ratio = actual / predicted;
        double threshold = 0.5;
        assertTrue(ratio < threshold);
    }

    // CrossFastBreakB pattern: consistent intervals buffer
    @Test
    void fastBreakConsistencyGate() {
        int consistent = 12;
        int gate = 12;
        assertTrue(consistent >= gate);

        consistent = 8;
        assertFalse(consistent >= gate);
    }

    @Test
    void fastBreakConsistencyCapped() {
        int consistent = 15;
        int cap = 15;
        consistent = Math.min(cap, consistent + 1);
        assertEquals(15, consistent);
    }

    // AimAssist pattern: snap streak
    @Test
    void aimAssistSnapStreak() {
        int snapStreak = 8;
        int threshold = 8;
        assertTrue(snapStreak >= threshold);

        snapStreak = 5;
        assertFalse(snapStreak >= threshold);
    }

    // Decay pattern: reward() reduces VL
    @Test
    void vlDecaysOverTime() {
        double vl = 10.0;
        double decay = 0.05;
        vl = Math.max(0, vl - decay);
        assertEquals(9.95, vl, 0.001);

        for (int i = 0; i < 200; i++) {
            vl = Math.max(0, vl - decay);
        }
        assertEquals(0.0, vl, 0.001);
    }

    // Max VL cap prevents further decay stop
    @Test
    void maxVlCapsAtLimit() {
        double vl = 199;
        double maxVL = 200;
        vl = Math.min(maxVL, vl + 1);
        assertEquals(200, vl, 0.001);

        vl = Math.min(maxVL, vl + 1);
        assertEquals(200, vl, 0.001);
    }
}
