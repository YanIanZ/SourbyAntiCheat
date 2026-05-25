package dev.yanianz.sourbyanticheat.profile;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public final class ProfileWorldMap {
    // Lock-free reads (hot path), copy-on-write adds (rare, enable-time). Order =
    // first-match precedence: file mappings inserted first, runtime adds appended.
    private final List<Map.Entry<Pattern, Profile>> compiled = new CopyOnWriteArrayList<>();
    private final Profile fallback;

    public ProfileWorldMap(LinkedHashMap<String, Profile> raw, Profile fallback) {
        this.fallback = fallback;
        for (var e : raw.entrySet()) {
            compiled.add(new AbstractMap.SimpleImmutableEntry<>(globToRegex(e.getKey()), e.getValue()));
        }
    }

    /** Append a runtime glob-&gt;profile mapping (lower precedence than existing entries). */
    public void addMapping(String glob, Profile profile) {
        if (glob == null || profile == null) return;
        compiled.add(new AbstractMap.SimpleImmutableEntry<>(globToRegex(glob), profile));
    }

    public Profile lookup(String worldName) {
        if (worldName == null) return fallback;
        for (var e : compiled) {
            if (e.getKey().matcher(worldName).matches()) return e.getValue();
        }
        return fallback;
    }

    private static Pattern globToRegex(String glob) {
        var sb = new StringBuilder("^");
        for (char c : glob.toCharArray()) {
            if (c == '*') sb.append(".*");
            else if ("\\.[](){}+?^$|".indexOf(c) >= 0) sb.append('\\').append(c);
            else sb.append(c);
        }
        return Pattern.compile(sb.append('$').toString());
    }
}
