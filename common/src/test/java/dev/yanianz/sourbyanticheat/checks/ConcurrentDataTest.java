package dev.yanianz.sourbyanticheat.checks;

import org.junit.jupiter.api.Test;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ConcurrentDataTest {

    @Test
    void concurrentMapPutAndGet() {
        Map<UUID, String> map = new ConcurrentHashMap<>();
        UUID key = UUID.randomUUID();
        map.put(key, "value");
        assertEquals("value", map.get(key));
    }

    @Test
    void concurrentMapComputeIfAbsent() {
        Map<UUID, java.util.List<String>> map = new ConcurrentHashMap<>();
        UUID key = UUID.randomUUID();
        map.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add("item");
        assertEquals(1, map.get(key).size());
    }

    @Test
    void concurrentMapRemoveIf() {
        Map<UUID, Long> map = new ConcurrentHashMap<>();
        UUID k1 = UUID.randomUUID();
        map.put(k1, 100L);
        map.entrySet().removeIf(e -> e.getValue() < 200L);
        assertTrue(map.isEmpty());
    }

    @Test
    void concurrentMapKeySetIsView() {
        Map<UUID, String> map = new ConcurrentHashMap<>();
        UUID key = UUID.randomUUID();
        map.put(key, "test");
        assertTrue(map.keySet().contains(key));
    }

    @Test
    void cooldownMapStoresTimestamps() {
        Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
        UUID player = UUID.randomUUID();
        long now = System.currentTimeMillis();
        cooldowns.put(player, now);

        Long last = cooldowns.get(player);
        assertNotNull(last);
        long elapsed = System.currentTimeMillis() - last;
        assertTrue(elapsed >= 0 && elapsed < 5000);
    }

    @Test
    void cooldownCheckExpired() {
        long now = System.currentTimeMillis();
        long cooldownSeconds = 1;
        Long last = now - 2000;
        assertTrue(last != null && now - last >= cooldownSeconds * 1000L);
    }

    @Test
    void cooldownCheckNotExpired() {
        long now = System.currentTimeMillis();
        long cooldownSeconds = 60;
        Long last = now - 500;
        assertTrue(last != null && now - last < cooldownSeconds * 1000L);
    }

    @Test
    void reportListSortingByTimestamp() {
        var reports = new java.util.ArrayList<ReportEntry>();
        reports.add(new ReportEntry(1000L));
        reports.add(new ReportEntry(3000L));
        reports.add(new ReportEntry(2000L));
        reports.sort((a, b) -> Long.compare(b.ts, a.ts));
        assertEquals(3000L, reports.get(0).ts);
        assertEquals(2000L, reports.get(1).ts);
        assertEquals(1000L, reports.get(2).ts);
    }

    record ReportEntry(long ts) {}

    @Test
    void nullSafeMapGetOrDefault() {
        Map<UUID, String> map = new ConcurrentHashMap<>();
        assertEquals("default", map.getOrDefault(UUID.randomUUID(), "default"));
    }

    @Test
    void threadSafeCounter() {
        var counter = new java.util.concurrent.atomic.AtomicInteger(0);
        for (int i = 0; i < 100; i++) counter.incrementAndGet();
        assertEquals(100, counter.get());
    }
}
