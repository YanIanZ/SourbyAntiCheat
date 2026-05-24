package dev.yanianz.sourbyanticheat.profile;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class ProfileRegistry {
    private final Function<UUID, Profile> resolver;
    private final ConcurrentHashMap<UUID, Profile> overrides = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Profile> cache = new ConcurrentHashMap<>();

    public ProfileRegistry(Function<UUID, Profile> resolver) {
        this.resolver = resolver;
    }

    public Profile get(UUID playerId) {
        Profile ov = overrides.get(playerId);
        if (ov != null) return ov;
        return cache.computeIfAbsent(playerId, resolver);
    }

    public void setOverride(UUID playerId, Profile profile) {
        if (profile == null) throw new IllegalArgumentException("profile must not be null");
        overrides.put(playerId, profile);
    }

    public void clearOverride(UUID playerId) {
        overrides.remove(playerId);
        cache.remove(playerId);
    }

    public void invalidate(UUID playerId) {
        cache.remove(playerId);
    }

    public void remove(UUID playerId) {
        overrides.remove(playerId);
        cache.remove(playerId);
    }
}
