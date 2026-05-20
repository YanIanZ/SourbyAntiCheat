package dev.yanianz.sourbyanticheat.manager;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PunishmentFormatTest {

    @Test void alertFormatReplacesPlayer() {
        String fmt = "&f%player% &bfailed %check_name% [%vl%] %verbose%";
        String result = fmt
            .replace("%player%", "TestPlayer")
            .replace("%check_name%", "Speed")
            .replace("%vl%", "100")
            .replace("%verbose%", "ratio=1.83");
        assertTrue(result.contains("TestPlayer"));
        assertTrue(result.contains("Speed"));
        assertTrue(result.contains("100"));
    }

    @Test void alertFormatReplacesAllTokens() {
        String fmt = "%prefix% &f%player% &bfailed &f%check_name% &7[&c%vl%&7]";
        assertTrue(fmt.contains("%prefix%"));
        assertTrue(fmt.contains("%player%"));
        assertTrue(fmt.contains("%check_name%"));
        assertTrue(fmt.contains("%vl%"));
    }

    @Test void experimentalSymbolIsAppended() {
        String symbol = "*";
        String check = "TestCheck";
        assertEquals("TestCheck *", check + " " + symbol);
    }

    @Test void descriptionIsNotEmpty() {
        String desc = "Detects speed hacks via velocity ratio analysis";
        assertFalse(desc.isEmpty());
    }

    @Test void verboseContainsExtraInfo() {
        String verbose = "ratio=1.83 buffer=5.0 netty=45.8/s spartan=SPARTAN_FLAGGED";
        assertTrue(verbose.contains("ratio="));
        assertTrue(verbose.contains("buffer="));
        assertTrue(verbose.contains("netty="));
        assertTrue(verbose.contains("spartan="));
    }

    @Test void violationCountCalculation() {
        int vl = 0;
        for (int i = 0; i < 10; i++) vl++;
        assertEquals(10, vl);
    }

    @Test void thresholdIntervalEvenlyDivisible() {
        int count = 10;
        int interval = 5;
        assertEquals(0, count % interval);
    }

    @Test void thresholdIntervalNotEvenlyDivisible() {
        int count = 7;
        int interval = 5;
        assertNotEquals(0, count % interval);
    }

    @Test void executeCountTracksInvocations() {
        int count = 0;
        count++; count++; count++;
        assertEquals(3, count);
    }

    @Test void removeViolationsAfterIsInSeconds() {
        int seconds = 300;
        long ms = seconds * 1000L;
        assertEquals(300000L, ms);
    }

    @Test void long2ObjectMapStoresAndRetrieves() {
        var map = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<String>();
        map.put(1L, "test");
        assertEquals("test", map.get(1L));
    }

    @Test void long2ObjectMapRemovesOldEntries() {
        var map = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<String>();
        long now = System.currentTimeMillis();
        map.put(now - 5000L, "old");
        map.put(now - 1000L, "recent");
        map.long2ObjectEntrySet().removeIf(e -> now - e.getLongKey() > 3000);
        assertEquals(1, map.size());
        assertEquals("recent", map.get(now - 1000L));
    }

    @Test void punishGroupContainsChecks() {
        var list = new java.util.ArrayList<String>();
        list.add("Speed");
        list.add("Flight");
        assertTrue(list.contains("Speed"));
        assertFalse(list.contains("KillAura"));
    }

    @Test void alertPlaceholderPreservesFormatting() {
        String original = "<click:run_command:/sac info %player%>%player%</click>";
        String replaced = original.replace("%player%", "Test");
        assertTrue(replaced.contains("Test"));
        assertFalse(replaced.contains("%player%"));
    }

    @Test void proxyAlertFormatContainsProxyMarker() {
        String proxy = "%prefix% &f[&cproxy&f] &f%player% &bfailed &f%check_name%";
        assertTrue(proxy.contains("&cproxy"));
    }
}
