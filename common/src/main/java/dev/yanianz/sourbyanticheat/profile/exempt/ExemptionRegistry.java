package dev.yanianz.sourbyanticheat.profile.exempt;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ref-counted, per-player set of actively-exempted check names. A plugin hook
 * calls {@link #start} when an ability begins and {@link #stop} when it ends;
 * overlapping abilities exempting the same check are ref-counted so an early
 * stop does not un-exempt while another holder is still active.
 *
 * <p>Thread-safe: hooks fire on the main thread, checks read from packet threads.
 */
public final class ExemptionRegistry {

    private final Map<UUID, Map<String, Integer>> counts = new ConcurrentHashMap<>();

    public void start(UUID player, String checkName) {
        if (player == null || checkName == null) return;
        counts.computeIfAbsent(player, k -> new ConcurrentHashMap<>())
                .merge(checkName, 1, Integer::sum);
    }

    public void stop(UUID player, String checkName) {
        if (player == null || checkName == null) return;
        Map<String, Integer> byCheck = counts.get(player);
        if (byCheck == null) return;
        byCheck.computeIfPresent(checkName, (k, v) -> v <= 1 ? null : v - 1);
    }

    public boolean active(UUID player, String checkName) {
        if (player == null || checkName == null) return false;
        Map<String, Integer> byCheck = counts.get(player);
        return byCheck != null && byCheck.getOrDefault(checkName, 0) > 0;
    }

    public void clear(UUID player) {
        if (player == null) return;
        counts.remove(player);
    }
}
