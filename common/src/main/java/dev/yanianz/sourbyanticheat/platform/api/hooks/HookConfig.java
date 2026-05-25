package dev.yanianz.sourbyanticheat.platform.api.hooks;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Per-plugin hook configuration: an enabled flag and a map of ability key ->
 * check names to exempt while that ability is active. Ability keys are matched
 * case-insensitively. Immutable.
 */
public final class HookConfig {

    private final boolean enabled;
    private final Map<String, List<String>> abilityChecks;

    public HookConfig(boolean enabled, Map<String, List<String>> abilityChecks) {
        this.enabled = enabled;
        this.abilityChecks = Map.copyOf(abilityChecks);
    }

    public boolean enabled() {
        return enabled;
    }

    /** Checks to exempt for the given ability key, or empty if disabled/unknown. */
    public List<String> checksFor(String ability) {
        if (!enabled || ability == null) return List.of();
        return abilityChecks.getOrDefault(ability.toLowerCase(Locale.ROOT), List.of());
    }
}
