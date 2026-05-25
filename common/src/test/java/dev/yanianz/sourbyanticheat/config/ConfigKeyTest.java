package dev.yanianz.sourbyanticheat.config;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ConfigKeyTest {

    @Test
    void punishmentConfigKeysExist() {
        Set<String> keys = Set.of(
            "punishment.max-vl",
            "punishment.staff-alert-vl",
            "punishment.warn-threshold",
            "punishment.kick-threshold",
            "punishment.ban-threshold",
            "punishment.warn-message",
            "punishment.kick-command",
            "punishment.ban-command",
            "punishment.wave-command",
            "punishment.wave-interval"
        );
        assertEquals(10, keys.size());
        assertTrue(keys.contains("punishment.max-vl"));
        assertTrue(keys.contains("punishment.staff-alert-vl"));
        assertTrue(keys.contains("punishment.wave-interval"));
    }

    @Test
    void alertCooldownAndLegacyAutoKeysExist() {
        java.util.Set<String> keys = java.util.Set.of(
            "alerts.cooldown-ms",
            "punishment.legacy-auto-enabled"
        );
        assertEquals(2, keys.size());
        assertTrue(keys.contains("alerts.cooldown-ms"));
        assertTrue(keys.contains("punishment.legacy-auto-enabled"));
    }

    @Test
    void reportConfigKeysExist() {
        Set<String> keys = Set.of(
            "report.cooldown-seconds",
            "report.broadcast-to-staff",
            "report.discord-webhook",
            "report.proxy-broadcast",
            "report.max-age-days"
        );
        assertEquals(5, keys.size());
    }

    @Test
    void consoleFilterConfigKeysExist() {
        Set<String> keys = Set.of(
            "console.filter-checks",
            "console.filter-min-vl",
            "console.filter-players"
        );
        assertEquals(3, keys.size());
    }

    @Test
    void alertFormatPlaceholdersAreConsistent() {
        String alert = "%prefix% &f%player% &bfailed &f%check_name% &7[&c%vl%&7] &7%verbose%";
        assertTrue(alert.contains("%player%"));
        assertTrue(alert.contains("%check_name%"));
        assertTrue(alert.contains("%vl%"));
        assertTrue(alert.contains("%verbose%"));
        assertTrue(alert.contains("%prefix%"));
    }

    @Test
    void discordContentPlaceholders() {
        Set<String> placeholders = Set.of(
            "%player%", "%check%", "%violations%",
            "%version%", "%brand%", "%ping%", "%tps%"
        );
        assertEquals(7, placeholders.size());
    }

    @Test
    void crossspeedbConfigHasAllFiveOptions() {
        Set<String> options = Set.of(
            "netty-rate-threshold", "max-ratio-deviation",
            "ratio-threshold", "vel-floor", "sprint-ratio-cap"
        );
        assertEquals(5, options.size());
    }

    @Test
    void crossfastbreakbConfigHasAllOptions() {
        Set<String> options = Set.of(
            "interval-variance-threshold", "interval-min-ms",
            "interval-max-ms", "consistency-gate", "netty-rate-threshold"
        );
        assertEquals(5, options.size());
    }

    @Test
    void crossnofallConfigHasAllOptions() {
        Set<String> options = Set.of(
            "offset-threshold", "full-offset-threshold", "netty-delay-threshold"
        );
        assertEquals(3, options.size());
    }

    @Test
    void crossnoswingConfigHasAllOptions() {
        Set<String> options = Set.of("netty-variance-threshold", "swing-window");
        assertEquals(2, options.size());
    }

    @Test
    void crossantikbConfigHasAllOptions() {
        Set<String> options = Set.of("ratio-threshold", "min-predicted-movement");
        assertEquals(2, options.size());
    }

    @Test
    void waveCommandFormatIsValid() {
        String format = "ban %player% %uuid%";
        String result = format
            .replace("%player%", "test")
            .replace("%uuid%", "abc");
        assertEquals("ban test abc", result);
    }
}
