package dev.yanianz.sourbyanticheat.integration;

import dev.yanianz.sourbyanticheat.profile.*;
import dev.yanianz.sourbyanticheat.profile.leniency.*;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class ProfileIntegrationTest {
    Path bakedYml = Path.of("src/main/resources/profiles.yml");

    @Test void bedwarsPearlGrantsSpeedLeniency() {
        var cfg = new ProfileConfig(bakedYml);
        cfg.reload();
        var clock = new AtomicLong(1000);
        var tracker = new LeniencyTracker(clock::get);
        var bus = new LeniencyEventBus(id -> Profile.BEDWARS, cfg::snapshot, tracker);
        UUID p = UUID.randomUUID();
        bus.fire(LeniencyId.ENDER_PEARL_LAND, p, 0);
        assertTrue(tracker.active("Speed", p));
        assertTrue(tracker.active("FlightA", p));
    }
    @Test void skywarsRodPullGrantsReachLeniency() {
        var cfg = new ProfileConfig(bakedYml);
        cfg.reload();
        var tracker = new LeniencyTracker(() -> 1000);
        var bus = new LeniencyEventBus(id -> Profile.SKYWARS, cfg::snapshot, tracker);
        UUID p = UUID.randomUUID();
        bus.fire(LeniencyId.ROD_PULL, p, 0);
        assertTrue(tracker.active("Reach", p));
    }
    @Test void skyblockElytraBoostGrantsFlightLeniency() {
        var cfg = new ProfileConfig(bakedYml);
        cfg.reload();
        var tracker = new LeniencyTracker(() -> 1000);
        var bus = new LeniencyEventBus(id -> Profile.SKYBLOCK, cfg::snapshot, tracker);
        UUID p = UUID.randomUUID();
        bus.fire(LeniencyId.ELYTRA_FIREWORK_BOOST, p, 0);
        assertTrue(tracker.active("FlightA", p));
        assertTrue(tracker.active("Speed", p));
    }
    @Test void lobbyDisablesAllCombatAndMovement() {
        var cfg = new ProfileConfig(bakedYml);
        cfg.reload();
        var section = cfg.snapshot().forProfile(Profile.LOBBY);
        for (String c : new String[]{"Reach","Speed","FlightA","Step","Spider","FastBreak"}) {
            assertTrue(section.isDisabled(c), c + " should be disabled in LOBBY");
        }
    }
    @Test void practiceTightensReachThreshold() {
        var cfg = new ProfileConfig(bakedYml);
        cfg.reload();
        var section = cfg.snapshot().forProfile(Profile.PRACTICE);
        assertEquals(3.05, ((Number) section.override("Reach", "threshold")).doubleValue(), 1e-9);
    }
}
