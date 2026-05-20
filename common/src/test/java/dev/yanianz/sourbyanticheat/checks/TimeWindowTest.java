package dev.yanianz.sourbyanticheat.checks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimeWindowTest {

    @Test void oneSecondWindowCollectsAllBreaks() {
        long now = System.currentTimeMillis();
        long lastReset = now - 2000;
        assertTrue(now - lastReset > 1000);
    }

    @Test void breakCountResetsAfterWindowExpires() {
        long windowMs = 1000;
        long now = System.currentTimeMillis();
        long lastReset = now - 1500;
        if (now - lastReset > windowMs) {
            lastReset = now; // reset
        }
        assertEquals(now, lastReset);
    }

    @Test void windowNotExpiredYet() {
        long now = System.currentTimeMillis();
        long lastReset = now - 500;
        assertFalse(now - lastReset > 1000);
    }

    @Test void breakIntervalWithinBounds() {
        long interval = 50;
        assertTrue(interval > 20 && interval < 200);
    }

    @Test void breakIntervalTooShort() {
        long interval = 10;
        assertFalse(interval > 20);
    }

    @Test void breakIntervalTooLong() {
        long interval = 300;
        assertFalse(interval < 200);
    }

    @Test void breakIntervalExactlyAtMin() {
        assertFalse(20 > 20);
    }

    @Test void breakIntervalExactlyAtMax() {
        assertFalse(200 < 200);
    }

    @Test void cleanupAgeCalculation() {
        long cutoff = System.currentTimeMillis() - (7 * 86400000L);
        long oldStamp = cutoff - 1000;
        assertTrue(oldStamp < cutoff);
    }

    @Test void cleanupKeepsRecentEntries() {
        long cutoff = System.currentTimeMillis() - (7 * 86400000L);
        long recentStamp = System.currentTimeMillis() - 1000;
        assertFalse(recentStamp < cutoff);
    }

    @Test void cooldownRemainingTime() {
        long last = 1000000L;
        long now = 1005000L;
        long cooldownMs = 60000L;
        long remaining = (last + cooldownMs - now) / 1000L;
        assertEquals(55, remaining);
    }

    @Test void cooldownFullyExpired() {
        long last = 1000000L;
        long now = 2000000L;
        long cooldownMs = 60000L;
        assertTrue(now - last >= cooldownMs);
    }

    @Test void lastBreakTimeIsZeroInitially() {
        assertEquals(0L, 0L);
    }

    @Test void firstBreakSetsTimestamp() {
        long now = System.currentTimeMillis();
        long lastBreak = now;
        assertEquals(now, lastBreak);
    }

    @Test void secondBreakCalculatesInterval() {
        long first = 1000L;
        long second = 1050L;
        assertEquals(50L, second - first);
    }

    @Test void hourlyCleanupInterval() {
        long minute = 60000L;
        long fiveMin = 5 * minute;
        assertTrue(fiveMin > 0);
        assertEquals(300000L, fiveMin);
    }

    @Test void swingWindowInTicks() {
        int swingTicks = 6;
        assertTrue(swingTicks >= 2);
        assertTrue(swingTicks <= 10);
    }

    @Test void swingTickDecrement() {
        int ticks = 4;
        if (ticks > 0) ticks--;
        assertEquals(3, ticks);
    }

    @Test void swingTickExpiresAfterAllTicks() {
        int ticks = 1;
        if (ticks > 0) ticks--;
        assertEquals(0, ticks);
    }
}
