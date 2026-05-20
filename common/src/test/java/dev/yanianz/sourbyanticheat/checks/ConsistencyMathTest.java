package dev.yanianz.sourbyanticheat.checks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConsistencyMathTest {

    // Tests the math behind consistency tracking in checks
    @Test
    void intervalConsistencyCounter() {
        int consistent = 0;
        long[] intervals = {45, 48, 50, 47, 52, 49, 200, 46, 51, 48};
        long min = 20, max = 200;

        for (long interval : intervals) {
            if (interval > min && interval < max) {
                consistent++;
            } else {
                consistent = Math.max(0, consistent - 3);
            }
        }
        // 200 at index 6 breaks consistency, -3 resets
        assertTrue(consistent >= 3);
    }

    @Test
    void consistentIntervalsCappedAtMax() {
        int consistent = 15;
        int cap = 15;
        consistent = Math.min(cap, consistent + 1);
        assertEquals(15, consistent);
    }

    @Test
    void runningAverageVariance() {
        double[] diffs = {5, 10, 15, 5, 10};
        double sum = 0;
        for (double d : diffs) sum += d;
        assertEquals(9.0, sum / diffs.length, 0.001);
    }

    @Test
    void varianceOfUniformDistribution() {
        double[] values = {10, 10, 10, 10, 10};
        double mean = 10;
        double var = 0;
        for (double v : values) var += (v - mean) * (v - mean);
        var /= values.length;
        assertEquals(0.0, var, 0.001);
    }

    @Test
    void varianceOfSpreadDistribution() {
        double[] values = {1, 5, 3, 7, 2, 8, 4, 6};
        double mean = 4.5;
        double var = 0;
        for (double v : values) var += (v - mean) * (v - mean);
        var /= values.length;
        assertTrue(var > 3.0);
    }

    @Test
    void yawDifferenceCalculation() {
        float a = 350f, b = 10f;
        float diff = Math.abs(a - b);
        if (diff > 180) diff = 360 - diff;
        assertEquals(20f, diff, 0.001);
    }

    @Test
    void yawDifferenceNoWrap() {
        float a = 45f, b = 90f;
        float diff = Math.abs(a - b);
        assertEquals(45f, diff, 0.001);
    }

    @Test
    void knocbackAntiRatioWithInfiniteActual() {
        double predicted = 0.5;
        double actual = 0.0;
        assertEquals(0.0, actual / predicted, 0.001);
    }

    @Test
    void ratioWithZeroPredicted() {
        double predicted = 0.0;
        double actual = 0.3;
        assertTrue(actual > 0 && predicted == 0);
    }

    @Test
    void exponentialMovingAverage() {
        double ema = 0;
        double alpha = 0.3;
        double[] values = {10, 12, 11, 13, 12, 14, 13, 15};

        for (double v : values) {
            ema = alpha * v + (1 - alpha) * ema;
        }
        assertTrue(ema > 10 && ema < 16);
    }
}
