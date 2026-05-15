package dev.yanianz.sourbyanticheat.spartan;

import dev.yanianz.sourbyanticheat.SacAPI;
import me.vagdedes.spartan.api.API;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spartan AntiCheat cross-check integration.
 * Uses the built-in SpartanAPI classes (me.vagdedes.spartan.api.*)
 * which are compiled directly into the SAC jar.
 *
 * The built-in API delegates to SAC's own violation data, so cross-check
 * works without requiring an external Spartan plugin. When the real
 * Spartan plugin is present, it will shadow our built-in classes and
 * provide its own data instead.
 *
 * @author YanIanZ
 */
public class SpartanCrossCheck {

    private static final Map<UUID, CrossCheckStats> stats = new ConcurrentHashMap<>();
    private static boolean spartanAvailable = false;
    private static boolean crossCheckEnabled = false;
    private static int minVL = 3;
    private static String spartanVersion = null;
    private static long startTime = System.currentTimeMillis();
    private static long totalFlags = 0;
    private static long spartanAgreements = 0;

    public static void init(boolean enabled) {
        crossCheckEnabled = enabled;
        startTime = System.currentTimeMillis();

        // The SpartanAPI classes are built-in (me.vagdedes.spartan.api.*),
        // so they are always available. We verify with a direct reference
        // rather than Class.forName() which would fail if the classes
        // were not properly compiled into the jar.
        try {
            // Touch the class to ensure it's loadable
            Class<?> eventClass = me.vagdedes.spartan.api.PlayerViolationEvent.class;
            spartanAvailable = (eventClass != null);
            try {
                spartanVersion = API.getVersion();
            } catch (Exception ignored) {}
        } catch (NoClassDefFoundError | Exception e) {
            spartanAvailable = false;
        }

        if (spartanAvailable && SacAPI.INSTANCE.getConfigManager() != null) {
            minVL = SacAPI.INSTANCE.getConfigManager().getConfig().getIntElse("spartanapi.min-vl", 3);
        }
    }

    public static CrossCheckResult checkSpartan(UUID playerUuid, String checkType) {
        if (!spartanAvailable || !crossCheckEnabled) return CrossCheckResult.NOT_AVAILABLE;
        totalFlags++;
        try {
            Object player = getBukkitPlayer(playerUuid);
            if (player == null) return CrossCheckResult.NOT_FOUND;

            int totalVL = API.getVL((org.bukkit.entity.Player) player);

            CrossCheckStats s = stats.computeIfAbsent(playerUuid, k -> new CrossCheckStats());
            if (totalVL > 0) {
                s.agreements++;
                spartanAgreements++;
                return new CrossCheckResult(CrossCheckResult.Type.SPARTAN_FLAGGED, totalVL);
            }
            s.disagreements++;
            return new CrossCheckResult(CrossCheckResult.Type.SPARTAN_CLEAN, 0);
        } catch (Exception e) {
            return CrossCheckResult.NOT_AVAILABLE;
        }
    }

    public static int getSpartanPerCheckVL(UUID playerUuid, String checkType) {
        if (!spartanAvailable) return 0;
        try {
            Object player = getBukkitPlayer(playerUuid);
            if (player == null) return 0;

            // Try to match the check type to a HackType enum
            for (me.vagdedes.spartan.system.Enums.HackType hackType : me.vagdedes.spartan.system.Enums.HackType.values()) {
                if (hackType.name().equalsIgnoreCase(checkType)) {
                    return API.getVL((org.bukkit.entity.Player) player, hackType);
                }
            }
            // Fallback to total VL
            return API.getVL((org.bukkit.entity.Player) player);
        } catch (Exception e) {
            return 0;
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
