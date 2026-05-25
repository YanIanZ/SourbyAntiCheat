package dev.yanianz.sourbyanticheat.profile;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;

class ProfileRegistryTest {
    UUID p = UUID.randomUUID();

    @Test void unknownPlayerReturnsGeneric() {
        var r = new ProfileRegistry(uuid -> Profile.GENERIC);
        assertEquals(Profile.GENERIC, r.get(p));
    }
    @Test void overrideTakesPrecedence() {
        var r = new ProfileRegistry(uuid -> Profile.SKYBLOCK);
        r.setOverride(p, Profile.BEDWARS);
        assertEquals(Profile.BEDWARS, r.get(p));
    }
    @Test void clearOverrideFallsBackToResolver() {
        var r = new ProfileRegistry(uuid -> Profile.SKYWARS);
        r.setOverride(p, Profile.BEDWARS);
        r.clearOverride(p);
        assertEquals(Profile.SKYWARS, r.get(p));
    }
    @Test void invalidateClearsCache() {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var r = new ProfileRegistry(uuid -> { calls.incrementAndGet(); return Profile.PRACTICE; });
        r.get(p); r.get(p);
        assertEquals(1, calls.get(), "second get should be cached");
        r.invalidate(p);
        r.get(p);
        assertEquals(2, calls.get(), "invalidate forces re-resolve");
    }
    @Test void setOverrideNullThrows() {
        var r = new ProfileRegistry(uuid -> Profile.GENERIC);
        assertThrows(IllegalArgumentException.class, () -> r.setOverride(p, null));
    }
    @Test void concurrentGetsThreadSafe() throws Exception {
        var r = new ProfileRegistry(uuid -> Profile.PRACTICE);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(8);
        var seen = ConcurrentHashMap.newKeySet();
        for (int i = 0; i < 1000; i++) pool.submit(() -> seen.add(r.get(p)));
        pool.shutdown(); pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        assertEquals(java.util.Set.of(Profile.PRACTICE), seen);
    }
}
