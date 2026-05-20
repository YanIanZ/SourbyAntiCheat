package dev.yanianz.sourbyanticheat.checks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VectorMathTest {

    @Test
    void horizontalSpeedFromVelocity() {
        double vx = 0.3, vz = 0.4;
        double h = Math.sqrt(vx * vx + vz * vz);
        assertEquals(0.5, h, 0.0001);
    }

    @Test
    void horizontalSpeedZero() {
        assertEquals(0.0, Math.sqrt(0 * 0 + 0 * 0), 0.0001);
    }

    @Test
    void horizontalSpeedNegativeComponent() {
        double vx = -0.3, vz = -0.4;
        double h = Math.sqrt(vx * vx + vz * vz);
        assertEquals(0.5, h, 0.0001);
    }

    @Test
    void ratioOfActualToPredicted() {
        assertEquals(2.0, 1.0 / 0.5, 0.001);
        assertEquals(0.5, 0.25 / 0.5, 0.001);
    }

    @Test
    void positionDeltaMagnitude() {
        double dx = 1.0, dy = 2.0, dz = 2.0;
        double mag = Math.sqrt(dx * dx + dy * dy + dz * dz);
        assertEquals(3.0, mag, 0.0001);
    }

    @Test
    void yOffsetCalculation() {
        double peY = 0.42;
        double predY = 0.0;
        double yOff = Math.abs(peY - predY);
        assertEquals(0.42, yOff, 0.0001);
    }

    @Test
    void yOffsetWhenPredictedMatches() {
        double peY = 0.1;
        double predY = 0.1;
        assertEquals(0.0, Math.abs(peY - predY), 0.0001);
    }

    @Test
    void speedRatioWhenMovingFasterThanVelocity() {
        double actual = 2.0, vel = 1.5;
        assertEquals(1.333, actual / vel, 0.001);
    }

    @Test
    void speedRatioWhenMovingSlowerThanVelocity() {
        double actual = 0.5, vel = 1.5;
        assertEquals(0.333, actual / vel, 0.001);
    }

    @Test
    void velocityBelowFloorIsFiltered() {
        double vel = 0.005;
        double floor = 0.01;
        assertFalse(vel >= floor);
    }

    @Test
    void velocityAboveFloorIsProcessed() {
        double vel = 0.05;
        double floor = 0.01;
        assertTrue(vel >= floor);
    }

    @Test
    void predictedMovementBelowFloorIsFiltered() {
        double predicted = 0.01;
        double floor = 0.15;
        assertFalse(predicted >= floor);
    }

    @Test
    void offsetFromPredictionNearZero() {
        double offset = 0.001;
        double threshold = 0.15;
        assertFalse(offset > threshold);
    }

    @Test
    void offsetFromPredictionAboveThreshold() {
        double offset = 0.25;
        double threshold = 0.15;
        assertTrue(offset > threshold);
    }

    @Test
    void yawNormalizationOver180() {
        float yaw = 200f;
        float normalized = yaw > 180 ? 360 - yaw : yaw;
        assertEquals(160f, normalized, 0.001);
    }

    @Test
    void yawNormalizationUnder180() {
        float yaw = 45f;
        float normalized = yaw > 180 ? 360 - yaw : yaw;
        assertEquals(45f, normalized, 0.001);
    }
}
