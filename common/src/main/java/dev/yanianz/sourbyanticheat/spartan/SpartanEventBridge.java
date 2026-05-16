package dev.yanianz.sourbyanticheat.spartan;

import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import me.vagdedes.spartan.api.PlayerViolationEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SpartanEventBridge {

    private static final boolean EVENTS_AVAILABLE;

    private static final Map<String, Long> lastFireTime = new ConcurrentHashMap<>();
    private static final long DEDUP_WINDOW_MS = 500;

    static {
        boolean available;
        try {
            Class<?> cls = PlayerViolationEvent.class;
            available = (cls != null);
        } catch (NoClassDefFoundError | Exception e) {
            available = false;
        }
        EVENTS_AVAILABLE = available;
    }

    public static void fireViolation(SacPlayer sacPlayer, String checkName, int violations, String verbose) {
        if (!EVENTS_AVAILABLE) return;

        String dedupKey = sacPlayer.uuid + ":" + checkName;
        long now = System.currentTimeMillis();
        Long lastTime = lastFireTime.get(dedupKey);
        if (lastTime != null && now - lastTime < DEDUP_WINDOW_MS) {
            return;
        }
        lastFireTime.put(dedupKey, now);

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
            return Bukkit.getPlayer(sp.uuid);
        } catch (Exception e) {
            return null;
        }
    }

    private SpartanEventBridge() {}
}
