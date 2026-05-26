package dev.yanianz.sourbyanticheat.platform.bukkit.hooks;

import dev.yanianz.sourbyanticheat.profile.oldcombat.OldCombatState;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * OldCombatMechanics compatibility. Presence-gated; reads OCM's own config.yml
 * (generic Bukkit, no API dependency) to learn if disable-attack-cooldown is on,
 * and applies the operator's disable-list. Populates {@link OldCombatState}.
 */
public final class OldCombatHook {

    private OldCombatHook() {}

    public static final String PLUGIN_NAME = "OldCombatMechanics";

    /**
     * @param disableList operator-configured checks to disable while OCM is present
     */
    public static void apply(OldCombatState state, List<String> disableList) {
        if (state == null) return;
        try {
            Plugin ocm = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
            if (ocm == null) return;

            boolean cooldownDisabled = ocm.getConfig().getBoolean("disable-attack-cooldown.enabled", false);
            state.setCpsRelaxed(cooldownDisabled);

            Set<String> disabled = new LinkedHashSet<>(disableList == null ? List.of() : disableList);
            state.setDisabledChecks(disabled);

            LogUtil.info("OldCombatMechanics detected: cps-relax=" + cooldownDisabled
                    + ", disabled=" + disabled);
        } catch (Throwable t) {
            LogUtil.warn("OldCombatMechanics hook failed: " + t);
        }
    }
}
