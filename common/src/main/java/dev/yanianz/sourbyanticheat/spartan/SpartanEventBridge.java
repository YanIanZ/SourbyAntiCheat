package dev.yanianz.sourbyanticheat.spartan;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import me.vagdedes.spartan.api.PlayerViolationEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
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

        UUID uuid = sacPlayer.uuid;
        if (Bukkit.isPrimaryThread()) {
            callEventSync(uuid, checkName, violations, verbose);
        } else {
            SacAPI.INSTANCE.getScheduler().getGlobalRegionScheduler().execute(
                SacAPI.INSTANCE.getGrimPlugin(),
                () -> callEventSync(uuid, checkName, violations, verbose)
            );
        }
    }

    private static void callEventSync(UUID uuid, String checkName, int violations, String verbose) {
        try {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) return;

            PlayerViolationEvent event = new PlayerViolationEvent(player, checkName, violations, verbose);
            Bukkit.getPluginManager().callEvent(event);
        } catch (Exception e) {
            LogUtil.warn("[SAC] Failed to fire Spartan PlayerViolationEvent: " + e.getMessage());
        }
    }

    private SpartanEventBridge() {}
}
