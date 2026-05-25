package dev.yanianz.sourbyanticheat.profile.leniency;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

public final class LeniencyTracker {
    private final LongSupplier clock;
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Long>> data = new ConcurrentHashMap<>();

    public LeniencyTracker() { this(System::currentTimeMillis); }
    public LeniencyTracker(LongSupplier clock) { this.clock = clock; }

    public void add(UUID playerId, String checkName, long durationMs) {
        if (durationMs <= 0) return;
        long expiry = clock.getAsLong() + durationMs;
        data.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
            .merge(checkName, expiry, Math::max);
    }

    public boolean active(String checkName, UUID playerId) {
        var m = data.get(playerId);
        if (m == null) return false;
        Long expiry = m.get(checkName);
        if (expiry == null) return false;
        if (clock.getAsLong() > expiry) { m.remove(checkName); return false; }
        return true;
    }

    public void revoke(UUID playerId, String checkName) {
        var m = data.get(playerId);
        if (m != null) m.remove(checkName);
    }

    public void removePlayer(UUID playerId) { data.remove(playerId); }
}
