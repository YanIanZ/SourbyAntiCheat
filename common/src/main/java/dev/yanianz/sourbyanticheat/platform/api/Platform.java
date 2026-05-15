package dev.yanianz.sourbyanticheat.platform.api;

public enum Platform {
    BUKKIT,
    FOLIA,
    BUNGEECORD,
    VELOCITY;

    public static Platform resolveByName(String name) {
        for (Platform platform : values()) {
            if (platform.name().equalsIgnoreCase(name)) return platform;
        }
        return null;
    }
}
