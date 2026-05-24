package dev.yanianz.sourbyanticheat.profile;

import java.util.Locale;

public enum Profile {
    BEDWARS, SKYWARS, SKYBLOCK, PRACTICE, LOBBY, GENERIC;

    public static Profile fromString(String s) {
        if (s == null) return GENERIC;
        try { return valueOf(s.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return GENERIC; }
    }
}
