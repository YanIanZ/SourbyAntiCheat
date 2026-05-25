package dev.yanianz.sourbyanticheat.profile.leniency;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class LeniencyTrackerTest {
    UUID p = UUID.randomUUID();

    @Test void addAndActive() {
        var clock = new AtomicLong(1000);
        var t = new LeniencyTracker(clock::get);
        t.add(p, "Speed", 500);
        assertTrue(t.active("Speed", p));
    }
    @Test void expires() {
        var clock = new AtomicLong(1000);
        var t = new LeniencyTracker(clock::get);
        t.add(p, "Speed", 500);
        clock.set(1600);
        assertFalse(t.active("Speed", p));
    }
    @Test void otherCheckUnaffected() {
        var clock = new AtomicLong(1000);
        var t = new LeniencyTracker(clock::get);
        t.add(p, "Speed", 500);
        assertFalse(t.active("FlightA", p));
    }
    @Test void durationZeroIgnored() {
        var clock = new AtomicLong(1000);
        var t = new LeniencyTracker(clock::get);
        t.add(p, "Speed", 0);
        assertFalse(t.active("Speed", p));
    }
    @Test void negativeDurationIgnored() {
        var clock = new AtomicLong(1000);
        var t = new LeniencyTracker(clock::get);
        t.add(p, "Speed", -100);
        assertFalse(t.active("Speed", p));
    }
    @Test void revokeClears() {
        var clock = new AtomicLong(1000);
        var t = new LeniencyTracker(clock::get);
        t.add(p, "Speed", 5000);
        t.revoke(p, "Speed");
        assertFalse(t.active("Speed", p));
    }
    @Test void removeAllForPlayer() {
        var clock = new AtomicLong(1000);
        var t = new LeniencyTracker(clock::get);
        t.add(p, "Speed", 5000);
        t.add(p, "FlightA", 5000);
        t.removePlayer(p);
        assertFalse(t.active("Speed", p));
        assertFalse(t.active("FlightA", p));
    }
    @Test void concurrentSafe() throws Exception {
        var clock = new AtomicLong(1000);
        var t = new LeniencyTracker(clock::get);
        var pool = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 1000; i++) {
            int idx = i;
            pool.submit(() -> t.add(p, "C" + (idx % 10), 5000));
        }
        pool.shutdown(); pool.awaitTermination(5, TimeUnit.SECONDS);
        for (int i = 0; i < 10; i++) assertTrue(t.active("C" + i, p));
    }
}
