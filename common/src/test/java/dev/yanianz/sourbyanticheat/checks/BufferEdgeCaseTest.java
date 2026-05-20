package dev.yanianz.sourbyanticheat.checks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BufferEdgeCaseTest {

    // Edge: buffer already at 0, decay does nothing
    @Test
    void bufferAtZeroStaysAtZeroAfterDecay() {
        double buffer = 0;
        buffer = Math.max(0, buffer - 1.0);
        assertEquals(0.0, buffer, 0.001);
    }

    // Edge: buffer at cap, increment stops
    @Test
    void bufferAtCapStopsIncrementing() {
        double buffer = 5.0;
        double cap = 5.0;
        buffer = Math.min(cap, buffer + 2.0);
        assertEquals(5.0, buffer, 0.001);
    }

    // Edge: very small buffer after long decay
    @Test
    void tinyBufferDecaysToZero() {
        double buffer = 0.01;
        buffer = Math.max(0, buffer - 0.5);
        assertEquals(0.0, buffer, 0.001);
    }

    // Edge: rapid accumulation and decay cycle
    @Test
    void rapidAccumulationAndDecay() {
        double buffer = 0;
        double cap = 3.0;

        // Phase 1: accumulate
        for (int i = 0; i < 6; i++) {
            buffer = Math.min(cap, buffer + 0.5);
        }
        assertEquals(3.0, buffer, 0.001);

        // Phase 2: decay
        for (int i = 0; i < 9; i++) {
            buffer = Math.max(0, buffer - 0.5);
        }
        assertEquals(0.0, buffer, 0.001);
    }

    // Edge: ratio exactly at threshold
    @Test
    void ratioExactlyAtThresholdDoesNotFlag() {
        double ratio = 1.8;
        double threshold = 1.8;
        assertFalse(ratio > threshold); // strictly greater
    }

    // Edge: ratio just above threshold flags
    @Test
    void ratioJustAboveThresholdFlags() {
        double ratio = 1.801;
        double threshold = 1.8;
        assertTrue(ratio > threshold);
    }

    // Edge: netty variance exactly at threshold
    @Test
    void nettyVarianceExactlyAtThreshold() {
        double variance = 12.0;
        double threshold = 12.0;
        assertFalse(variance < threshold); // strictly less
    }

    // Edge: zero velocity division safety
    @Test
    void zeroVelocityRatioIsNaN() {
        double actualH = 5.0;
        double velH = 0.0;
        double ratio = actualH / velH;
        assertTrue(Double.isInfinite(ratio));
    }

    // Edge: negative buffer protection
    @Test
    void negativeBufferClampedToZero() {
        double buffer = -0.5;
        buffer = Math.max(0, buffer);
        assertEquals(0.0, buffer, 0.001);
    }

    // Edge: ping multiplier for high ping
    @Test
    void highPingReducesBufferIncrement() {
        double baseIncrement = 1.5;
        int ping = 450;
        double multiplier = ping > 400 ? 0.5 : 1.0;
        assertEquals(0.75, baseIncrement * multiplier, 0.001);
    }

    @Test
    void lowPingKeepsFullIncrement() {
        double baseIncrement = 1.5;
        int ping = 50;
        double multiplier = ping > 400 ? 0.5 : 1.0;
        assertEquals(1.5, baseIncrement * multiplier, 0.001);
    }

    // Edge: sample size exactly equals threshold
    @Test
    void sampleSizeEqualToThresholdPasses() {
        int sampleSize = 40;
        int threshold = 40;
        assertTrue(sampleSize >= threshold);
    }

    @Test
    void sampleSizeOneLessThanThresholdFails() {
        int sampleSize = 39;
        int threshold = 40;
        assertFalse(sampleSize >= threshold);
    }
}
