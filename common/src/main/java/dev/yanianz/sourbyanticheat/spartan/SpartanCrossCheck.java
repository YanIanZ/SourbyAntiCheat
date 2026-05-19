package dev.yanianz.sourbyanticheat.spartan;

import dev.yanianz.sourbyanticheat.SacAPI;
import me.vagdedes.spartan.api.API;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpartanCrossCheck {

    private static final Map<UUID, CrossCheckStats> stats = new ConcurrentHashMap<>();
    private static boolean spartanAvailable = false;
    private static boolean crossCheckEnabled = false;
    private static int minVL = 3;
    private static double minAgreementRate = 0.6;
    private static String spartanVersion = null;
    private static long startTime = System.currentTimeMillis();
    private static long totalFlags = 0;
    private static long spartanAgreements = 0;

    private static final Map<UUID, Map<String, CachedVL>> vlCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5000;

    private record CachedVL(int vl, long timestamp) {}

    public static void init(boolean enabled) {
        crossCheckEnabled = enabled;
        startTime = System.currentTimeMillis();

        try {
            Class<?> eventClass = me.vagdedes.spartan.api.PlayerViolationEvent.class;
            spartanAvailable = (eventClass != null);
            try {
                spartanVersion = API.getVersion();
            } catch (Exception ignored) {}
        } catch (NoClassDefFoundError | Exception e) {
            spartanAvailable = false;
        }

        if (spartanAvailable && SacAPI.INSTANCE.getConfigManager() != null) {
            minVL = SacAPI.INSTANCE.getConfigManager().getConfig().getIntElse("spartanapi.cross-check.min-vl", 3);
            minAgreementRate = SacAPI.INSTANCE.getConfigManager().getConfig().getDoubleElse("spartanapi.cross-check.min-agreement-rate", 0.6);
        }
    }

    public static CrossCheckResult checkSpartan(UUID playerUuid, String checkType) {
        if (!spartanAvailable || !crossCheckEnabled) return CrossCheckResult.NOT_AVAILABLE;
        totalFlags++;
        try {
            Object player = getBukkitPlayer(playerUuid);
            if (player == null) return CrossCheckResult.NOT_FOUND;

            // Per-check VL only. A cross-check for "Speed" must confirm against Spartan's
            // Speed violation level, NOT the player's total VL across every check —
            // using total VL made every crossapi check cross-confirm (and flag) the
            // moment a player had any Spartan violation of any unrelated type.
            int perCheckVL = getSpartanPerCheckVL(playerUuid, checkType);

            CrossCheckStats s = stats.computeIfAbsent(playerUuid, k -> new CrossCheckStats());
            if (perCheckVL > 0) {
                s.agreements++;
                spartanAgreements++;
                return new CrossCheckResult(CrossCheckResult.Type.SPARTAN_FLAGGED, perCheckVL);
            }
            s.disagreements++;
            return new CrossCheckResult(CrossCheckResult.Type.SPARTAN_CLEAN, 0);
        } catch (Exception e) {
            return CrossCheckResult.NOT_AVAILABLE;
        }
    }

    public static int getSpartanPerCheckVL(UUID playerUuid, String checkType) {
        if (!spartanAvailable) return 0;

        int cached = getCachedVL(playerUuid, checkType);
        if (cached >= 0) return cached;

        try {
            Object player = getBukkitPlayer(playerUuid);
            if (player == null) return 0;

            for (me.vagdedes.spartan.system.Enums.HackType hackType : me.vagdedes.spartan.system.Enums.HackType.values()) {
                if (hackType.name().equalsIgnoreCase(checkType)) {
                    int vl = API.getVL((org.bukkit.entity.Player) player, hackType);
                    updateCache(playerUuid, checkType, vl);
                    return vl;
                }
            }
            // No Spartan HackType matches this check — Spartan does not detect it, so it
            // cannot cross-confirm. Return 0 (clean) rather than the player's total VL.
            updateCache(playerUuid, checkType, 0);
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int getCachedVL(UUID uuid, String checkType) {
        Map<String, CachedVL> playerCache = vlCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        CachedVL cached = playerCache.get(checkType);
        if (cached != null && System.currentTimeMillis() - cached.timestamp() < CACHE_TTL_MS) {
            return cached.vl();
        }
        return -1;
    }

    private static void updateCache(UUID uuid, String checkType, int vl) {
        Map<String, CachedVL> playerCache = vlCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        playerCache.put(checkType, new CachedVL(vl, System.currentTimeMillis()));
    }

    private static Object getBukkitPlayer(UUID uuid) {
        try {
            return org.bukkit.Bukkit.getPlayer(uuid);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isAvailable() { return spartanAvailable && crossCheckEnabled; }
    public static int getMinVL() { return minVL; }
    public static String getSpartanVersion() { return spartanVersion; }
    public static long getTotalFlags() { return totalFlags; }
    public static long getAgreements() { return spartanAgreements; }
    public static double getAgreementRate() {
        return totalFlags == 0 ? 0 : (double) spartanAgreements / totalFlags;
    }

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
