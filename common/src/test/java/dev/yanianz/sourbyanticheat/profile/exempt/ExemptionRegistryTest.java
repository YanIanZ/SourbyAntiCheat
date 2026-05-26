package dev.yanianz.sourbyanticheat.profile.exempt;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ExemptionRegistryTest {

    private final UUID p = UUID.randomUUID();

    @Test
    void inactiveByDefault() {
        assertFalse(new ExemptionRegistry().active(p, "Speed"));
    }

    @Test
    void startMakesActive() {
        ExemptionRegistry r = new ExemptionRegistry();
        r.start(p, "Speed");
        assertTrue(r.active(p, "Speed"));
        assertFalse(r.active(p, "Reach"));
    }

    @Test
    void stopAfterSingleStartClears() {
        ExemptionRegistry r = new ExemptionRegistry();
        r.start(p, "Speed");
        r.stop(p, "Speed");
        assertFalse(r.active(p, "Speed"));
    }

    @Test
    void refCountedOverlappingHolders() {
        ExemptionRegistry r = new ExemptionRegistry();
        r.start(p, "Speed");
        r.start(p, "Speed"); // two holders
        r.stop(p, "Speed");
        assertTrue(r.active(p, "Speed"), "still held by second activation");
        r.stop(p, "Speed");
        assertFalse(r.active(p, "Speed"));
    }

    @Test
    void stopBelowZeroFloorsAtZero() {
        ExemptionRegistry r = new ExemptionRegistry();
        r.stop(p, "Speed"); // no prior start
        assertFalse(r.active(p, "Speed"));
        r.start(p, "Speed");
        assertTrue(r.active(p, "Speed"));
    }

    @Test
    void clearRemovesAllForPlayer() {
        ExemptionRegistry r = new ExemptionRegistry();
        r.start(p, "Speed");
        r.start(p, "Reach");
        r.clear(p);
        assertFalse(r.active(p, "Speed"));
        assertFalse(r.active(p, "Reach"));
    }

    @Test
    void nullSafe() {
        ExemptionRegistry r = new ExemptionRegistry();
        assertFalse(r.active(null, "Speed"));
        assertFalse(r.active(p, null));
        r.stop(p, "Speed"); // no throw
        r.clear(null);       // no throw
    }
}
