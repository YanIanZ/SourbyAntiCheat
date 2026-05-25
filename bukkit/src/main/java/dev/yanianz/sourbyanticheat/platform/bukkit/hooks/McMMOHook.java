package dev.yanianz.sourbyanticheat.platform.bukkit.hooks;

import com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityActivateEvent;
import com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityDeactivateEvent;
import dev.yanianz.sourbyanticheat.platform.api.hooks.HookConfig;
import dev.yanianz.sourbyanticheat.profile.exempt.ExemptionRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.UUID;

/**
 * Exempts the configured checks while an mcMMO super-ability is active.
 * mcMMO super-abilities have clean activate/deactivate events, so exemptions
 * are ref-counted start->stop with no timer needed.
 */
public final class McMMOHook implements PluginHook, Listener {

    private ExemptionRegistry exemptions;
    private HookConfig config;

    @Override
    public String pluginName() {
        return "mcMMO";
    }

    @Override
    public void register(JavaPlugin plugin, ExemptionRegistry exemptions, HookConfig config) {
        this.exemptions = exemptions;
        this.config = config;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onActivate(McMMOPlayerAbilityActivateEvent event) {
        toggle(event.getPlayer(), abilityKey(event.getAbility()), true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeactivate(McMMOPlayerAbilityDeactivateEvent event) {
        toggle(event.getPlayer(), abilityKey(event.getAbility()), false);
    }

    private static String abilityKey(Object ability) {
        return ability == null ? "" : ability.toString().toLowerCase(Locale.ROOT);
    }

    private void toggle(Player player, String abilityKey, boolean start) {
        if (player == null || exemptions == null || config == null) return;
        UUID uuid = player.getUniqueId();
        for (String check : config.checksFor(abilityKey)) {
            if (start) exemptions.start(uuid, check);
            else exemptions.stop(uuid, check);
        }
    }
}
