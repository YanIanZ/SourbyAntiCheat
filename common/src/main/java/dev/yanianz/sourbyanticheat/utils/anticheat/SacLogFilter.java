package dev.yanianz.sourbyanticheat.utils.anticheat;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Filter;
import java.util.logging.LogRecord;

public class SacLogFilter implements Filter {
    private static final long DEDUP_WINDOW_MS = 5000;
    private static final int MAX_REPEATS_PER_WINDOW = 3;
    private static final int CLEANUP_THRESHOLD = 2048;
    private final ConcurrentHashMap<String, DedupEntry> dedupMap = new ConcurrentHashMap<>();

    @Override
    public boolean isLoggable(LogRecord record) {
        String msg = record.getMessage();
        if (msg == null) return true;
        if (!msg.startsWith("[SAC] ") && !msg.startsWith("SAC") && !msg.contains("[SAC]")) {
            record.setMessage("[SAC] " + msg);
        }
        String key = record.getLevel() + ":" + record.getMessage();
        long now = System.currentTimeMillis();

        if (dedupMap.size() > CLEANUP_THRESHOLD) {
            Iterator<Map.Entry<String, DedupEntry>> it = dedupMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, DedupEntry> e = it.next();
                if ((now - e.getValue().timestamp) > DEDUP_WINDOW_MS) {
                    it.remove();
                }
            }
        }

        DedupEntry entry = dedupMap.compute(key, (k, existing) -> {
            if (existing == null || (now - existing.timestamp) > DEDUP_WINDOW_MS) {
                return new DedupEntry(now);
            }
            existing.count.incrementAndGet();
            return existing;
        });
        return entry.count.get() <= MAX_REPEATS_PER_WINDOW;
    }

    private static class DedupEntry {
        final long timestamp;
        final AtomicInteger count = new AtomicInteger(1);

        DedupEntry(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
