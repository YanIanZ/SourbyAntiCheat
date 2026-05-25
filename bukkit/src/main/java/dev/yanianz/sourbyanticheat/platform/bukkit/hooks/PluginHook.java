package dev.yanianz.sourbyanticheat.platform.bukkit.hooks;

import dev.yanianz.sourbyanticheat.platform.api.hooks.HookConfig;
import dev.yanianz.sourbyanticheat.profile.exempt.ExemptionRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** A soft-depend integration. Only registered when its plugin is present. */
public interface PluginHook {

    /** Bukkit plugin name as it appears in plugin.yml (PluginManager lookup). */
    String pluginName();

    default boolean isPresent() {
        return Bukkit.getPluginManager().getPlugin(pluginName()) != null;
    }

    /** Register this hook's Bukkit listener(s). Called only when present + enabled. */
    void register(JavaPlugin plugin, ExemptionRegistry exemptions, HookConfig config);
}
