package dev.yanianz.sourbyanticheat.command;

import java.util.Set;

/**
 * Classifies command names as "legacy" — diagnostic / one-off / integration
 * commands that are hidden from the default command surface and only
 * registered when {@code commands.legacy-enabled: true}. Core commands are
 * everything not listed here. Disable-not-delete: the command classes remain.
 */
public final class CommandCatalog {

    public static final Set<String> LEGACY = Set.of(
            "perf",
            "debug",
            "sendalert",
            "history-migrate",
            "history-copy",
            "log",
            "dump",
            "testwebhook",
            "spartan",
            "summary",
            "checks"
    );

    public static boolean isLegacy(String name) {
        return name != null && LEGACY.contains(name);
    }

    private CommandCatalog() {}
}
