package dev.yanianz.sourbyanticheat.profile;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProfileConfigWriterTest {

    private Path write(Path dir, String yaml) throws Exception {
        Path f = dir.resolve("profiles.yml");
        Files.writeString(f, yaml);
        return f;
    }

    private static final String BASE = """
            config-version: 2
            profiles:
              LOBBY:
                disabled: [Reach]
                overrides: {}
                leniency: []
              BEDWARS:
                disabled: []
                overrides:
                  Speed: { maxvl: 10 }
                leniency: []
            """;

    @Test
    void addDisabledCheck() throws Exception {
        Path f = write(Files.createTempDirectory("p"), BASE);
        new ProfileConfigWriter(f).toggleDisabled(Profile.LOBBY, "Speed", true);

        ProfileConfig cfg = new ProfileConfig(f);
        cfg.reload();
        assertTrue(cfg.snapshot().forProfile(Profile.LOBBY).isDisabled("Speed"));
        assertTrue(cfg.snapshot().forProfile(Profile.LOBBY).isDisabled("Reach"));
    }

    @Test
    void removeDisabledCheck() throws Exception {
        Path f = write(Files.createTempDirectory("p"), BASE);
        new ProfileConfigWriter(f).toggleDisabled(Profile.LOBBY, "Reach", false);

        ProfileConfig cfg = new ProfileConfig(f);
        cfg.reload();
        assertFalse(cfg.snapshot().forProfile(Profile.LOBBY).isDisabled("Reach"));
    }

    @Test
    void setOverrideValue() throws Exception {
        Path f = write(Files.createTempDirectory("p"), BASE);
        new ProfileConfigWriter(f).setOverride(Profile.BEDWARS, "Speed", "maxvl", 20);

        ProfileConfig cfg = new ProfileConfig(f);
        cfg.reload();
        Object v = cfg.snapshot().forProfile(Profile.BEDWARS).override("Speed", "maxvl");
        assertEquals(20, ((Number) v).intValue());
    }

    @Test
    void setOverrideCreatesCheckEntry() throws Exception {
        Path f = write(Files.createTempDirectory("p"), BASE);
        new ProfileConfigWriter(f).setOverride(Profile.LOBBY, "Reach", "threshold", 3.5);

        ProfileConfig cfg = new ProfileConfig(f);
        cfg.reload();
        Object v = cfg.snapshot().forProfile(Profile.LOBBY).override("Reach", "threshold");
        assertEquals(3.5, ((Number) v).doubleValue(), 1e-9);
    }

    @Test
    void writePreservesOtherProfiles() throws Exception {
        Path f = write(Files.createTempDirectory("p"), BASE);
        new ProfileConfigWriter(f).toggleDisabled(Profile.LOBBY, "Speed", true);

        ProfileConfig cfg = new ProfileConfig(f);
        cfg.reload();
        assertEquals(10, ((Number) cfg.snapshot().forProfile(Profile.BEDWARS)
                .override("Speed", "maxvl")).intValue());
    }
}
