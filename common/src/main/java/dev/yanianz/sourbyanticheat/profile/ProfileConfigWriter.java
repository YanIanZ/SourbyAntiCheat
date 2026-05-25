package dev.yanianz.sourbyanticheat.profile;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Targeted editor for {@code profiles.yml}. Loads the whole document, mutates
 * the requested profile section in place, and writes it back. {@code profiles.yml}
 * carries no comments, so a snakeyaml round-trip is lossless for content.
 * Block style + 2-space indent keeps the file readable.
 */
public final class ProfileConfigWriter {

    private final Path file;

    public ProfileConfigWriter(Path file) {
        this.file = file;
    }

    /** Add or remove {@code checkName} from a profile's {@code disabled} list. */
    @SuppressWarnings("unchecked")
    public synchronized void toggleDisabled(Profile profile, String checkName, boolean disabled) throws IOException {
        Map<String, Object> root = load();
        Map<String, Object> section = sectionFor(root, profile);
        List<Object> list = section.get("disabled") instanceof List<?> l
                ? new ArrayList<>((List<Object>) l) : new ArrayList<>();
        list.remove(checkName);
        if (disabled) list.add(checkName);
        section.put("disabled", list);
        save(root);
    }

    /** Set {@code overrides.<checkName>.<key> = value} for a profile. */
    @SuppressWarnings("unchecked")
    public synchronized void setOverride(Profile profile, String checkName, String key, Object value) throws IOException {
        Map<String, Object> root = load();
        Map<String, Object> section = sectionFor(root, profile);
        Map<String, Object> overrides = section.get("overrides") instanceof Map<?, ?> m
                ? new LinkedHashMap<>((Map<String, Object>) m) : new LinkedHashMap<>();
        Map<String, Object> check = overrides.get(checkName) instanceof Map<?, ?> cm
                ? new LinkedHashMap<>((Map<String, Object>) cm) : new LinkedHashMap<>();
        check.put(key, value);
        overrides.put(checkName, check);
        section.put("overrides", overrides);
        save(root);
    }

    /** Remove an override key (and the check entry if it becomes empty). */
    @SuppressWarnings("unchecked")
    public synchronized void removeOverride(Profile profile, String checkName, String key) throws IOException {
        Map<String, Object> root = load();
        Map<String, Object> section = sectionFor(root, profile);
        if (!(section.get("overrides") instanceof Map<?, ?> m)) return;
        Map<String, Object> overrides = new LinkedHashMap<>((Map<String, Object>) m);
        if (overrides.get(checkName) instanceof Map<?, ?> cm) {
            Map<String, Object> check = new LinkedHashMap<>((Map<String, Object>) cm);
            check.remove(key);
            if (check.isEmpty()) overrides.remove(checkName);
            else overrides.put(checkName, check);
            section.put("overrides", overrides);
            save(root);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> load() throws IOException {
        Object raw = Files.exists(file)
                ? new Yaml().load(Files.newBufferedReader(file)) : null;
        Map<String, Object> root = raw instanceof Map<?, ?> m
                ? new LinkedHashMap<>((Map<String, Object>) m) : new LinkedHashMap<>();
        if (!(root.get("profiles") instanceof Map<?, ?>)) {
            root.put("profiles", new LinkedHashMap<String, Object>());
        }
        return root;
    }

    /** Returns the LIVE section map for {@code profile}, creating it (and the
     *  enclosing profiles map entry) if absent, so callers mutate the tree
     *  that {@link #save} will write. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> sectionFor(Map<String, Object> root, Profile profile) {
        Map<String, Object> profiles = (Map<String, Object>) root.get("profiles");
        Object existing = profiles.get(profile.name());
        if (existing instanceof Map<?, ?>) {
            // Re-wrap into a mutable map and store it back so it is the live node.
            Map<String, Object> live = new LinkedHashMap<>((Map<String, Object>) existing);
            profiles.put(profile.name(), live);
            return live;
        }
        Map<String, Object> fresh = new LinkedHashMap<>();
        fresh.put("disabled", new ArrayList<>());
        fresh.put("overrides", new LinkedHashMap<>());
        fresh.put("leniency", new ArrayList<>());
        profiles.put(profile.name(), fresh);
        return fresh;
    }

    private void save(Map<String, Object> root) throws IOException {
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setIndent(2);
        opts.setPrettyFlow(true);
        Yaml yaml = new Yaml(opts);
        try (Writer w = Files.newBufferedWriter(file)) {
            yaml.dump(root, w);
        }
    }
}
