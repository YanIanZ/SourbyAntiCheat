package dev.yanianz.sourbyanticheat.profile.leniency;

import dev.yanianz.sourbyanticheat.profile.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class LeniencyEventBusTest {
    UUID p = UUID.randomUUID();

    static ProfileConfigSnapshot snapWith(Profile prof, ProfileConfigSnapshot.LeniencyEntry... entries) {
        var section = new ProfileConfigSnapshot.ProfileSection(
                Set.of(), Map.of(), List.of(entries));
        return new ProfileConfigSnapshot(new EnumMap<>(Map.of(prof, section)));
    }

    @Test void fireGrantsLeniencyForMappedChecks() {
        var clock = new AtomicLong(1000);
        var tracker = new LeniencyTracker(clock::get);
        var snap = snapWith(Profile.BEDWARS, new ProfileConfigSnapshot.LeniencyEntry(
                "ENDER_PEARL_LAND", List.of("Speed", "FlightA"), 5000, false));
        var bus = new LeniencyEventBus(uuid -> Profile.BEDWARS, () -> snap, tracker);
        bus.fire(LeniencyId.ENDER_PEARL_LAND, p, 0);
        assertTrue(tracker.active("Speed", p));
        assertTrue(tracker.active("FlightA", p));
    }
    @Test void unknownProfileNoOp() {
        var tracker = new LeniencyTracker(() -> 1000);
        var snap = snapWith(Profile.BEDWARS, new ProfileConfigSnapshot.LeniencyEntry(
                "ENDER_PEARL_LAND", List.of("Speed"), 5000, false));
        var bus = new LeniencyEventBus(uuid -> Profile.SKYBLOCK, () -> snap, tracker);
        bus.fire(LeniencyId.ENDER_PEARL_LAND, p, 0);
        assertFalse(tracker.active("Speed", p));
    }
    @Test void scaleByAmpMultipliesDuration() {
        var clock = new AtomicLong(0);
        var tracker = new LeniencyTracker(clock::get);
        var snap = snapWith(Profile.SKYWARS, new ProfileConfigSnapshot.LeniencyEntry(
                "KIT_POTION_APPLY", List.of("Speed"), 1000, true));
        var bus = new LeniencyEventBus(uuid -> Profile.SKYWARS, () -> snap, tracker);
        bus.fire(LeniencyId.KIT_POTION_APPLY, p, 2);  // amp 2 → (2+1)x = 3000ms
        clock.set(2500);
        assertTrue(tracker.active("Speed", p), "should still be active at t=2500 (expiry ~3000)");
        clock.set(3500);
        assertFalse(tracker.active("Speed", p));
    }
    @Test void zeroDurationIgnored() {
        var tracker = new LeniencyTracker(() -> 1000);
        var snap = snapWith(Profile.BEDWARS, new ProfileConfigSnapshot.LeniencyEntry(
                "ENDER_PEARL_LAND", List.of("Speed"), 0, false));
        var bus = new LeniencyEventBus(uuid -> Profile.BEDWARS, () -> snap, tracker);
        bus.fire(LeniencyId.ENDER_PEARL_LAND, p, 0);
        assertFalse(tracker.active("Speed", p));
    }
}
