package dev.yanianz.sourbyanticheat.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigMigratorV1ToV2Test {
    @TempDir Path tmp;

    @Test void migrateKeepsOnlyKeptChecksInPracticeOverrides() throws Exception {
        Path f = tmp.resolve("checks.yml");
        Files.writeString(f, """
                config-version: 1
                checks:
                  Reach: { threshold: 3.1, maxvl: 4 }
                  AimSnap: { threshold: 99 }
                  Speed: { threshold: 1.2 }
                  ForceField: { threshold: 0.5 }
                """);
        new ConfigMigratorV1ToV2().migrate(f);
        var loaded = (Map<String, Object>) new Yaml().load(Files.newBufferedReader(f));
        assertEquals(2, loaded.get("config-version"));
        var profiles = (Map<String, Map<String, Object>>) loaded.get("profiles");
        var practice = profiles.get("PRACTICE");
        var overrides = (Map<String, Map<String, Object>>) practice.get("overrides");
        assertTrue(overrides.containsKey("Reach"));
        assertTrue(overrides.containsKey("Speed"));
        assertFalse(overrides.containsKey("AimSnap"));
        assertFalse(overrides.containsKey("ForceField"));
    }

    @Test void writesBackupBeforeMigrating() throws Exception {
        Path f = tmp.resolve("checks.yml");
        Files.writeString(f, "config-version: 1\nchecks: { Reach: { threshold: 3.1 } }\n");
        new ConfigMigratorV1ToV2().migrate(f);
        assertTrue(Files.exists(tmp.resolve("checks.v1.bak.yml")));
    }

    @Test void idempotentWhenAlreadyV2() throws Exception {
        Path f = tmp.resolve("checks.yml");
        Files.writeString(f, "config-version: 2\nprofiles: { GENERIC: {} }\n");
        byte[] before = Files.readAllBytes(f);
        new ConfigMigratorV1ToV2().migrate(f);
        assertArrayEquals(before, Files.readAllBytes(f));
    }

    @Test void unreadableFileDoesNotModifyOriginal() throws Exception {
        Path f = tmp.resolve("checks.yml");
        Files.writeString(f, "::: invalid :::");
        byte[] before = Files.readAllBytes(f);
        new ConfigMigratorV1ToV2().migrate(f);
        assertArrayEquals(before, Files.readAllBytes(f));
        assertTrue(Files.exists(tmp.resolve("checks.yml.failed-migration")));
    }

    @Test void missingFileNoOp() throws Exception {
        Path f = tmp.resolve("does-not-exist.yml");
        assertDoesNotThrow(() -> new ConfigMigratorV1ToV2().migrate(f));
        assertFalse(Files.exists(f));
    }
}
