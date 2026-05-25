package dev.yanianz.sourbyanticheat.profile;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ProfileConfig {
    private static final Logger LOG = Logger.getLogger(ProfileConfig.class.getName());

    private final Path file;
    private final AtomicReference<ProfileConfigSnapshot> snapshot =
            new AtomicReference<>(emptySnapshot());

    public ProfileConfig(Path file) { this.file = file; }

    public ProfileConfigSnapshot snapshot() { return snapshot.get(); }

    /** The backing {@code profiles.yml} path (used by the GUI editor). */
    public Path path() { return file; }

    @SuppressWarnings("unchecked")
    public void reload() {
        try {
            if (!Files.exists(file)) {
                LOG.info("profiles.yml not found; using built-in defaults");
                snapshot.set(emptySnapshot());
                return;
            }
            var raw = new Yaml().load(Files.newBufferedReader(file));
            if (!(raw instanceof Map<?, ?> root)) {
                LOG.severe("profiles.yml root is not a map; using defaults");
                snapshot.set(emptySnapshot());
                return;
            }
            var profilesNode = (Map<String, Map<String, Object>>) root.get("profiles");
            if (profilesNode == null) { snapshot.set(emptySnapshot()); return; }

            var built = new EnumMap<Profile, ProfileConfigSnapshot.ProfileSection>(Profile.class);
            for (var entry : profilesNode.entrySet()) {
                Profile p;
                try { p = Profile.valueOf(entry.getKey().toUpperCase(Locale.ROOT)); }
                catch (IllegalArgumentException e) {
                    LOG.warning("unknown profile name in profiles.yml: " + entry.getKey());
                    continue;
                }
                built.put(p, parseSection(entry.getValue()));
            }
            snapshot.set(new ProfileConfigSnapshot(built));
        } catch (IOException | RuntimeException ex) {
            LOG.log(Level.SEVERE, "failed to parse profiles.yml; using defaults", ex);
            snapshot.set(emptySnapshot());
        }
    }

    @SuppressWarnings("unchecked")
    private static ProfileConfigSnapshot.ProfileSection parseSection(Map<String, Object> node) {
        if (node == null) return new ProfileConfigSnapshot.ProfileSection(Set.of(), Map.of(), List.of());
        Set<String> disabled = node.get("disabled") instanceof List<?> dl
                ? new LinkedHashSet<>((List<String>) dl) : Set.of();
        Map<String, Map<String, Object>> overrides = node.get("overrides") instanceof Map<?, ?> ov
                ? (Map<String, Map<String, Object>>) ov : Map.of();
        var leniencyList = new ArrayList<ProfileConfigSnapshot.LeniencyEntry>();
        if (node.get("leniency") instanceof List<?> ll) {
            for (Object item : ll) {
                if (!(item instanceof Map<?, ?> m)) continue;
                String ev = (String) m.get("event");
                List<String> checks = m.get("checks") instanceof List<?> cl
                        ? (List<String>) cl : List.of();
                long dur = m.get("duration_ms") instanceof Number n ? n.longValue() : 0L;
                boolean amp = "amp".equals(m.get("scale"));
                if (ev == null) continue;
                leniencyList.add(new ProfileConfigSnapshot.LeniencyEntry(ev, checks, dur, amp));
            }
        }
        return new ProfileConfigSnapshot.ProfileSection(disabled, overrides, leniencyList);
    }

    private static ProfileConfigSnapshot emptySnapshot() {
        return new ProfileConfigSnapshot(new EnumMap<>(Profile.class));
    }
}
