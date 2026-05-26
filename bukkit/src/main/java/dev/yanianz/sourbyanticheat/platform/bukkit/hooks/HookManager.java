package dev.yanianz.sourbyanticheat.platform.bukkit.hooks;

import dev.yanianz.sourbyanticheat.platform.api.hooks.HookConfig;
import dev.yanianz.sourbyanticheat.profile.exempt.ExemptionRegistry;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.function.Function;

/** Registers every {@link PluginHook} whose plugin is present and enabled. */
public final class HookManager {

    private final List<PluginHook> hooks;

    public HookManager(List<PluginHook> hooks) {
        this.hooks = hooks;
    }

    /**
     * @param configFor resolves a hook's {@link HookConfig} by plugin key
     *                  (lowercased plugin name)
     */
    public void enablePresent(JavaPlugin plugin, ExemptionRegistry exemptions,
                              Function<String, HookConfig> configFor) {
        for (PluginHook hook : hooks) {
            try {
                HookConfig cfg = configFor.apply(hook.pluginName().toLowerCase(java.util.Locale.ROOT));
                if (cfg == null || !cfg.enabled() || !hook.isPresent()) continue;
                hook.register(plugin, exemptions, cfg);
                LogUtil.info("Hooked into " + hook.pluginName());
            } catch (Throwable t) {
                // One bad hook must never abort plugin enable or other hooks.
                LogUtil.warn("Failed to register hook " + hook.pluginName() + ": " + t);
            }
        }
    }
}
