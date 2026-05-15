package dev.yanianz.sourbyanticheat.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks per-check execution time for performance monitoring.
 */
public final class CheckPerformance {

    private static final Map<String, AtomicLong> totalNanos = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> callCount = new ConcurrentHashMap<>();

    public static void record(String checkName, long nanos) {
        totalNanos.computeIfAbsent(checkName, k -> new AtomicLong()).addAndGet(nanos);
        callCount.computeIfAbsent(checkName, k -> new AtomicLong()).incrementAndGet();
    }

    public static double getAvgMicros(String checkName) {
        long total = totalNanos.getOrDefault(checkName, new AtomicLong()).get();
        long calls = callCount.getOrDefault(checkName, new AtomicLong()).get();
        return calls == 0 ? 0 : (total / 1000.0) / calls;
    }

    public static Map<String, Double> getAllAvgMicros() {
        Map<String, Double> result = new ConcurrentHashMap<>();
        totalNanos.forEach((k, v) -> {
            long calls = callCount.getOrDefault(k, new AtomicLong()).get();
            result.put(k, calls == 0 ? 0 : (v.get() / 1000.0) / calls);
        });
        return result;
    }

    public static void reset() {
        totalNanos.clear();
        callCount.clear();
    }

    private CheckPerformance() {}
}
