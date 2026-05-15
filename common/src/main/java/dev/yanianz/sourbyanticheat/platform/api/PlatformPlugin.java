package dev.yanianz.sourbyanticheat.platform.api;

public interface PlatformPlugin {
    boolean isEnabled();

    String getName();

    String getVersion();
}
