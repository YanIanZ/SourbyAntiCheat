package dev.yanianz.sourbyanticheat.profile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class ProfileWorldMap {
    private final Map<Pattern, Profile> compiled = new LinkedHashMap<>();
    private final Profile fallback;

    public ProfileWorldMap(LinkedHashMap<String, Profile> raw, Profile fallback) {
        this.fallback = fallback;
        for (var e : raw.entrySet()) {
            compiled.put(globToRegex(e.getKey()), e.getValue());
        }
    }

    public Profile lookup(String worldName) {
        if (worldName == null) return fallback;
        for (var e : compiled.entrySet()) {
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
