package dev.yanianz.sourbyanticheat.utils.anticheat;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Filter;
import java.util.logging.LogRecord;

public class SacLogFilter implements Filter {
    private static final long DEDUP_WINDOW_MS = 5000;
    private static final int MAX_REPEATS_PER_WINDOW = 3;
    private final ConcurrentMap<String, DedupEntry> dedupMap = new ConcurrentHashMap<>();
    private final String sacPrefix = "[SAC] ";

    @Override
    public boolean isLoggable(LogRecord record) {
        String msg = record.getMessage();
        if (msg == null) return true;
        if (!msg.startsWith(sacPrefix) && !msg.startsWith("SAC") && !msg.contains("[SAC]")) {
            record.setMessage(sacPrefix + msg);
        }
        String key = record.getLevel() + ":" + record.getMessage();
        DedupEntry entry = dedupMap.compute(key, (k, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || (now - existing.timestamp) > DEDUP_WINDOW_MS) {
                return new DedupEntry(now);
            }
            existing.count.incrementAndGet();
            return existing;
        });
        if (entry.count.get() > MAX_REPEATS_PER_WINDOW) {
            return false;
        }
        return true;
    }

    private static class DedupEntry {
        final long timestamp;
        final AtomicInteger count = new AtomicInteger(1);

        DedupEntry(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
