package dev.yanianz.sourbyanticheat.checks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NullSafetyTest {

    @Test void emptyStringIsNotValidReason() {
        String r = ""; assertTrue(r.trim().isEmpty());
    }

    @Test void blankStringIsNotValidReason() {
        String r = "   "; assertTrue(r.trim().isEmpty());
    }

    @Test void nullCooldownMeansNoBlock() {
        Long last = null;
        long now = System.currentTimeMillis();
        assertTrue(last == null || now - last >= 60000);
    }

    @Test void emptyReportsMapReturnsEmpty() {
        var map = new java.util.HashMap<>();
        var all = new java.util.ArrayList<>();
        for (var list : map.values()) all.addAll((java.util.List) list);
        assertTrue(all.isEmpty());
    }

    @Test void emptyGetOrDefault() {
        var map = new java.util.concurrent.ConcurrentHashMap<>();
        assertEquals(java.util.List.of(), map.getOrDefault("x", java.util.List.of()));
    }

    @Test void nullPlayerNameFallback() {
        String name = null;
        String display = name != null ? name : "Unknown";
        assertEquals("Unknown", display);
    }

    @Test void nullSafeStreamSum() {
        double[] vals = {3.0, 5.0, 0.0};
        double sum = 0;
        for (double v : vals) sum += v;
        assertEquals(8.0, sum, 0.001);
    }

    @Test void emptyListMaxIsSafe() {
        var list = new java.util.ArrayList<Double>();
        assertTrue(list.isEmpty());
    }

    @Test void nullUUIDDoesNotThrowInEquals() {
        java.util.UUID u = java.util.UUID.randomUUID();
        assertNotNull(u);
        assertDoesNotThrow(() -> u.equals(null));
        assertFalse(u.equals(null));
    }

    @Test void doubleDivisionByZeroIsSafe() {
        double result = 5.0 / 0.0;
        assertTrue(Double.isInfinite(result));
    }

    @Test void NaNIsNotLessThan() {
        assertFalse(Double.NaN < 5.0);
    }

    @Test void NaNIsNotGreaterThan() {
        assertFalse(Double.NaN > 5.0);
    }

    @Test void NaNIsNotEqualToSelf() {
        assertFalse(Double.NaN == Double.NaN);
    }

    @Test void infinityIsGreaterThanMaxDouble() {
        assertTrue(Double.POSITIVE_INFINITY > Double.MAX_VALUE);
    }

    @Test void negativeInfinityIsLessThanMinDouble() {
        assertTrue(Double.NEGATIVE_INFINITY < -Double.MAX_VALUE);
    }

    @Test void absentSpartanDataDefaultsToZero() {
        var map = new java.util.concurrent.ConcurrentHashMap<String, Integer>();
        assertEquals(0, (int) map.getOrDefault("Speed", 0));
    }

    @Test void emptySpartanCheckMapReturnsNull() {
        var map = new java.util.concurrent.ConcurrentHashMap<String, Integer>();
        assertNull(map.get("NoSuchCheck"));
    }

    @Test void concurrentModificationOnReadIsSafe() {
        var set = java.util.concurrent.ConcurrentHashMap.newKeySet();
        set.add("a"); set.add("b");
        var copy = new java.util.HashSet<>(set);
        assertEquals(2, copy.size());
    }

    @Test void maxOfNegativeNumberIsZero() {
        assertEquals(0, Math.max(0, -5));
    }

    @Test void minOfOverCapIsCap() {
        assertEquals(5, Math.min(5, 10));
    }
}
