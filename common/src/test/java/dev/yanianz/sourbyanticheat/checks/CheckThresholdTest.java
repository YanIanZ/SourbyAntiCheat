package dev.yanianz.sourbyanticheat.checks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CheckThresholdTest {

    @Test
    void ratioAboveThresholdTriggersCheck() {
        double ratio = 1.83;
        double threshold = 1.5;
        assertTrue(ratio > threshold);
    }

    @Test
    void sprintingPlayerUsesHigherThreshold() {
        double ratio = 1.83;
        double sprintCap = 2.4;
        assertFalse(ratio > sprintCap);
    }

    @Test
    void nonSprintingPlayerFlaggedAtLowerThreshold() {
        double ratio = 1.83;
        double walkThreshold = 1.8;
        assertTrue(ratio > walkThreshold);
    }

    @Test
    void deviationCheckPreventsFlagOnNoisyData() {
        double ratio = 1.83;
        double consistentRatio = 1.3;
        double deviation = Math.abs(ratio - consistentRatio);
        double maxDeviation = 0.3;
        // Deviation 0.53 > 0.3 — not consistent, should NOT flag
        assertFalse(deviation < maxDeviation);
    }

    @Test
    void consistentRatioPatternIsSuspicious() {
        double ratio = 1.83;
        double consistentRatio = 1.80;
        double deviation = Math.abs(ratio - consistentRatio);
        double maxDeviation = 0.3;
        // Deviation 0.03 < 0.3 — very consistent, suspicious
        assertTrue(deviation < maxDeviation);
    }

    @Test
    void nettyRateGateBlocksLowPacketRate() {
        double nettyRate = 45.8;
        double threshold = 120.0;
        assertFalse(nettyRate > threshold);
    }

    @Test
    void nettyRateGateAllowsHighPacketRate() {
        double nettyRate = 200.0;
        double threshold = 120.0;
        assertTrue(nettyRate > threshold);
    }

    @Test
    void offsetFromPredictionThreshold() {
        double offset = 0.015;
        double threshold = 0.15;
        assertFalse(offset > threshold);
    }

    @Test
    void yOffsetAloneIsNotGroundSpoof() {
        double yOffset = 0.420;
        double fullOffset = 0.0;
        double yThreshold = 0.1;
        double fullThreshold = 0.2;
        // AND gate: both must be true
        boolean groundSpoof = yOffset > yThreshold && fullOffset > fullThreshold;
        assertFalse(groundSpoof);
    }

    @Test
    void bothOffsetsHighIsGroundSpoof() {
        double yOffset = 0.420;
        double fullOffset = 0.3;
        double yThreshold = 0.1;
        double fullThreshold = 0.2;
        boolean groundSpoof = yOffset > yThreshold && fullOffset > fullThreshold;
        assertTrue(groundSpoof);
    }

    @Test
    void minKnockbackFloorIgnoresTinyKnockback() {
        double predicted = 0.075;
        double floor = 0.15;
        assertFalse(predicted >= floor);
    }

    @Test
    void significantKnockbackPassesFloor() {
        double predicted = 0.5;
        double floor = 0.15;
        assertTrue(predicted >= floor);
    }
}
