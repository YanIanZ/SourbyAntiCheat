package dev.yanianz.sourbyanticheat.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProfileConfigLoaderTest {
    @TempDir Path tmp;

    private Path write(String name, String content) throws Exception {
        Path p = tmp.resolve(name);
        Files.writeString(p, content);
        return p;
    }

    @Test void parsesBedwarsProfile() throws Exception {
        var f = write("profiles.yml", """
                config-version: 2
                profiles:
                  BEDWARS:
                    disabled: [Spider]
                    overrides:
                      FastBreak: { threshold: 0.85, maxvl: 5 }
                    leniency:
                      - { event: ENDER_PEARL_LAND, checks: [Speed], duration_ms: 5000 }
                """);
        var cfg = new ProfileConfig(f);
        cfg.reload();
        var section = cfg.snapshot().forProfile(Profile.BEDWARS);
        assertTrue(section.isDisabled("Spider"));
        assertEquals(0.85, ((Number) section.override("FastBreak", "threshold")).doubleValue(), 1e-9);
        assertEquals(1, section.leniencies.size());
        assertEquals("ENDER_PEARL_LAND", section.leniencies.get(0).event());
        assertEquals(5000L, section.leniencies.get(0).durationMs());
    }

    @Test void malformedYamlFallsBackToDefaults() throws Exception {
        var f = write("profiles.yml", "this : is : invalid : yaml :::");
        var cfg = new ProfileConfig(f);
        cfg.reload();
        for (Profile p : Profile.values()) {
            var s = cfg.snapshot().forProfile(p);
            assertNotNull(s);
            assertTrue(s.disabled.isEmpty());
        }
    }

    @Test void missingFileLoadsDefaults() throws Exception {
        var cfg = new ProfileConfig(tmp.resolve("does-not-exist.yml"));
        cfg.reload();
        assertNotNull(cfg.snapshot().forProfile(Profile.GENERIC));
    }

    @Test void unknownProfileNameLogsAndSkips() throws Exception {
        var f = write("profiles.yml", """
                config-version: 2
                profiles:
                  NOTAPROFILE:
                    disabled: [X]
                """);
        var cfg = new ProfileConfig(f);
        cfg.reload();
        for (Profile p : Profile.values()) {
            assertFalse(cfg.snapshot().forProfile(p).isDisabled("X"));
        }
    }

    @Test void reloadSwapsSnapshotAtomically() throws Exception {
        var f = write("profiles.yml", """
                config-version: 2
                profiles:
                  BEDWARS: { disabled: [Spider] }
                """);
        var cfg = new ProfileConfig(f);
        cfg.reload();
        var snap1 = cfg.snapshot();
        Files.writeString(f, """
                config-version: 2
                profiles:
                  BEDWARS: { disabled: [Tower] }
                """);
        cfg.reload();
        var snap2 = cfg.snapshot();
        assertTrue(snap1.forProfile(Profile.BEDWARS).isDisabled("Spider"));
        assertTrue(snap2.forProfile(Profile.BEDWARS).isDisabled("Tower"));
        assertFalse(snap2.forProfile(Profile.BEDWARS).isDisabled("Spider"));
    }
}
