package dev.yanianz.sourbyanticheat.spartan;

import dev.yanianz.sourbyanticheat.SacAPI;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spartan AntiCheat cross-check integration.
 * Uses the real Spartan API (me.vagdedes.spartan.api.API) compiled from spartan-api.jar.
 * Detection via Class.forName — safe fallback if Spartan not installed.
 *
 * @author YanIanZ
 */
public class SpartanCrossCheck {

    private static final Map<UUID, CrossCheckStats> stats = new ConcurrentHashMap<>();
    private static boolean spartanAvailable = false;
    private static boolean crossCheckEnabled = false;
    private static int minVL = 3;

    public static void init(boolean enabled) {
        crossCheckEnabled = enabled;
        try {
            Class.forName("me.vagdedes.spartan.api.API");
            spartanAvailable = true;
        } catch (ClassNotFoundException e) {
            spartanAvailable = false;
        }
        if (spartanAvailable && SacAPI.INSTANCE.getConfigManager() != null) {
            minVL = SacAPI.INSTANCE.getConfigManager().getConfig().getIntElse("spartanapi.min-vl", 3);
        }
    }

    public static CrossCheckResult checkSpartan(UUID playerUuid, String checkType) {
        if (!spartanAvailable || !crossCheckEnabled) return CrossCheckResult.NOT_AVAILABLE;
        try {
            Class<?> apiClass = Class.forName("me.vagdedes.spartan.api.API");
            Object player = getBukkitPlayer(playerUuid);
            if (player == null) return CrossCheckResult.NOT_FOUND;

            int totalVL = (int) apiClass.getMethod("getVL", org.bukkit.entity.Player.class)
                .invoke(null, player);

            CrossCheckStats s = stats.computeIfAbsent(playerUuid, k -> new CrossCheckStats());
            if (totalVL > 0) {
                s.agreements++;
                return new CrossCheckResult(CrossCheckResult.Type.SPARTAN_FLAGGED, totalVL);
            }
            s.disagreements++;
            return new CrossCheckResult(CrossCheckResult.Type.SPARTAN_CLEAN, 0);
        } catch (Exception e) {
            return CrossCheckResult.NOT_AVAILABLE;
        }
    }

    private static Object getBukkitPlayer(UUID uuid) {
        try {
            var pdm = SacAPI.INSTANCE.getPlayerDataManager();
            var sp = pdm.getPlayer(uuid);
            if (sp == null || sp.platformPlayer == null) return null;
            return sp.platformPlayer.getClass().getMethod("getPlayer").invoke(sp.platformPlayer);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isAvailable() { return spartanAvailable && crossCheckEnabled; }
    public static int getMinVL() { return minVL; }

    public static CrossCheckStats getStats(UUID playerUuid) {
        return stats.getOrDefault(playerUuid, new CrossCheckStats());
    }

    public record CrossCheckResult(Type type, int spartanVL) {
        public enum Type { NOT_AVAILABLE, NOT_FOUND, SPARTAN_CLEAN, SPARTAN_FLAGGED }
        public static final CrossCheckResult NOT_AVAILABLE = new CrossCheckResult(Type.NOT_AVAILABLE, 0);
        public static final CrossCheckResult NOT_FOUND = new CrossCheckResult(Type.NOT_FOUND, 0);
    }

    public static class CrossCheckStats {
        public int agreements = 0;
        public int disagreements = 0;
        public double agreementRate() {
            int total = agreements + disagreements;
            return total == 0 ? 0 : (double) agreements / total;
        }
    }
}
