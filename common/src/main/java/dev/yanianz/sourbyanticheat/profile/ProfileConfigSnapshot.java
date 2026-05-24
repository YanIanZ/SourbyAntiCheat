package dev.yanianz.sourbyanticheat.profile;

import java.util.*;

public final class ProfileConfigSnapshot {
    public record LeniencyEntry(String event, List<String> checks, long durationMs, boolean scaleByAmp) {}

    public static final class ProfileSection {
        public final Set<String> disabled;
        public final Map<String, Map<String, Object>> overrides;
        public final List<LeniencyEntry> leniencies;

        public ProfileSection(Set<String> disabled,
                              Map<String, Map<String, Object>> overrides,
                              List<LeniencyEntry> leniencies) {
            this.disabled = Set.copyOf(disabled);
            this.overrides = Map.copyOf(overrides);
            this.leniencies = List.copyOf(leniencies);
        }
        public boolean isDisabled(String checkName) { return disabled.contains(checkName); }
        public Object override(String checkName, String key) {
            var m = overrides.get(checkName);
            return m == null ? null : m.get(key);
        }
    }

    private final Map<Profile, ProfileSection> sections;

    public ProfileConfigSnapshot(Map<Profile, ProfileSection> sections) {
        var copy = new EnumMap<Profile, ProfileSection>(Profile.class);
        copy.putAll(sections);
        // ensure all profiles have an entry (empty defaults)
        for (Profile p : Profile.values()) {
            copy.putIfAbsent(p, new ProfileSection(Set.of(), Map.of(), List.of()));
        }
        this.sections = Collections.unmodifiableMap(copy);
    }

    public ProfileSection forProfile(Profile p) { return sections.get(p); }
}
