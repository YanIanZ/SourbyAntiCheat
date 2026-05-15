package dev.yanianz.sourbyanticheat.spartan;

import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import me.vagdedes.spartan.api.PlayerViolationEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Bridges SAC violations to SpartanAPI events.
 * Plugins listening for me.vagdedes.spartan.api.PlayerViolationEvent
 * will receive them from SAC.
 *
 * Uses the built-in PlayerViolationEvent class directly instead of
 * reflection, since it's compiled into the SAC jar.
 */
public final class SpartanEventBridge {

    // Always available since PlayerViolationEvent is built into the jar
    private static final boolean EVENTS_AVAILABLE;

    static {
        boolean available;
        try {
            // Touch the class to verify it's loadable
            Class<?> cls = PlayerViolationEvent.class;
            available = (cls != null);
        } catch (NoClassDefFoundError | Exception e) {
            available = false;
        }
        EVENTS_AVAILABLE = available;
    }

    public static void fireViolation(SacPlayer sacPlayer, String checkName, int violations, String verbose) {
        if (!EVENTS_AVAILABLE) return;
        try {
            Player player = getBukkitPlayer(sacPlayer);
            if (player == null) return;

            PlayerViolationEvent event = new PlayerViolationEvent(player, checkName, violations, verbose);
            Bukkit.getPluginManager().callEvent(event);
        } catch (Exception e) {
            LogUtil.warn("Failed to fire Spartan PlayerViolationEvent: " + e.getMessage());
        }
    }

    private static Player getBukkitPlayer(SacPlayer sp) {
        try {
            if (sp.platformPlayer == null) return null;
            return (Player) sp.platformPlayer.getClass().getMethod("getPlayer").invoke(sp.platformPlayer);
        } catch (Exception e) {
            return null;
        }
    }

    private SpartanEventBridge() {}
}
