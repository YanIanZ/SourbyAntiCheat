package dev.yanianz.sourbyanticheat.spartan;

import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Bridges SAC violations to SpartanAPI events.
 * Plugins listening for me.vagdedes.spartan.api.PlayerViolationEvent
 * will receive them from SAC.
 */
public final class SpartanEventBridge {

    private static boolean eventsAvailable = false;

    static {
        try {
            Class.forName("me.vagdedes.spartan.api.PlayerViolationEvent");
            eventsAvailable = true;
        } catch (ClassNotFoundException ignored) {}
    }

    public static void fireViolation(SacPlayer sacPlayer, String checkName, int violations, String verbose) {
        if (!eventsAvailable) return;
        try {
            Player player = getBukkitPlayer(sacPlayer);
            if (player == null) return;

            Class<?> eventClass = Class.forName("me.vagdedes.spartan.api.PlayerViolationEvent");
            Object event = eventClass.getConstructor(Player.class, String.class, int.class, String.class)
                .newInstance(player, checkName, violations, verbose);
            Bukkit.getPluginManager().callEvent((org.bukkit.event.Event) event);
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
