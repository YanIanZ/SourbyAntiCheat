package dev.yanianz.sourbyanticheat.profile.leniency;

import dev.yanianz.sourbyanticheat.profile.Profile;
import dev.yanianz.sourbyanticheat.profile.ProfileConfigSnapshot;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public final class LeniencyEventBus {
    private final Function<UUID, Profile> profileOf;
    private final Supplier<ProfileConfigSnapshot> snapshotSupplier;
    private final LeniencyTracker tracker;

    public LeniencyEventBus(Function<UUID, Profile> profileOf,
                            Supplier<ProfileConfigSnapshot> snapshotSupplier,
                            LeniencyTracker tracker) {
        this.profileOf = profileOf;
        this.snapshotSupplier = snapshotSupplier;
        this.tracker = tracker;
    }

    public void fire(LeniencyId id, UUID playerId, int amplifier) {
        Profile p = profileOf.apply(playerId);
        var section = snapshotSupplier.get().forProfile(p);
        for (var entry : section.leniencies) {
            if (!entry.event().equals(id.name())) continue;
            long duration = entry.scaleByAmp() ? entry.durationMs() * (amplifier + 1L) : entry.durationMs();
            for (String check : entry.checks()) tracker.add(playerId, check, duration);
        }
    }
}
