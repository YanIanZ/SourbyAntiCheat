package me.vagdedes.spartan.api;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.checks.Check;
import me.vagdedes.spartan.system.Enums.HackType;
import me.vagdedes.spartan.system.Enums.Permission;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.UUID;

@SuppressWarnings("unused")
public class BackgroundAPI {

    private static dev.yanianz.sourbyanticheat.player.SacPlayer sp(Player p) {
        try { return SacAPI.INSTANCE.getPlayerDataManager().getPlayer(p.getUniqueId()); }
        catch (Exception e) { return null; }
    }

    private static int sacVL(Player p) {
        var s = sp(p); return s == null ? 0 : (int) s.checkManager.allChecks.values().stream().mapToDouble(c -> ((Check) c).violations).sum();
    }

    static String licenseID() { return "SAC-API"; }
    static String getVersion() { return SacAPI.INSTANCE.getSacVersion(); }
    static String getMessage(String path) { return null; }
    static boolean getSetting(String path) { return false; }
    static int getViolationResetTime() { return 60; }
    static String getCategory(Player p, HackType hackType) { return hackType.name(); }
    static boolean isEnabled(HackType hackType) { return true; }
    static boolean isSilent(HackType hackType) { return false; }
    static int getVL(Player p, HackType hackType) { return sacVL(p); }
    static double getCertainty(Player p, HackType hackType) { return 0.5; }
    static double getDecimalVL(Player p, HackType hackType) { return sacVL(p); }
    static int getVL(Player p) { return sacVL(p); }
    static void setVL(Player p, HackType hackType, int amount) {}
    static int getCancelViolation(HackType hackType, String worldName) { return 0; }
    static int getCancelViolation(HackType hackType) { return 0; }
    static double getDecimalVL(Player p) { return sacVL(p); }
    static int getPing(Player p) { return p.getPing(); }
    static double getTPS() { return 20.0; }
    static boolean hasPermission(Player p, Permission perm) { return p.isOp(); }
    static void reloadConfig() {}
    static void reloadPermissions() {}
    static void reloadPermissions(Player p) {}
    static void enableCheck(HackType hackType) {}
    static void disableCheck(HackType hackType) {}
    static void enableSilentChecking(Player p, HackType hackType) {}
    static void disableSilentChecking(Player p, HackType hackType) {}
    static void enableSilentChecking(HackType hackType) {}
    static void disableSilentChecking(HackType hackType) {}
    static void cancelCheck(Player p, HackType hackType, int ticks) {}
    static void cancelCheckPerVerbose(Player p, String s, int ticks) {}
    static void startCheck(Player p, HackType hackType) {}
    static void stopCheck(Player p, HackType hackType) {}
    static void resetVL() {}
    static void resetVL(Player p) {}
    static boolean isBypassing(Player p) { return false; }
    static boolean isBypassing(Player p, HackType hackType) { return false; }
    static void banPlayer(UUID uuid, String reason) {}
    static boolean isBanned(UUID uuid) { return false; }
    static void unbanPlayer(UUID uuid) {}
    static String getBanReason(UUID uuid) { return null; }
    static String getBanPunisher(UUID uuid) { return null; }
    static boolean isHacker(Player p) { return sacVL(p) > 100; }
    static boolean isLegitimate(Player p) { return sacVL(p) < 30; }
    static boolean hasMiningNotificationsEnabled(Player p) { return false; }
    static void setMiningNotifications(Player p, boolean value) {}
    static int getCPS(Player p) { return 0; }
    static UUID[] getBanList() { return new UUID[0]; }
    static boolean addToWave(UUID uuid, String command) { return false; }
    static void removeFromWave(UUID uuid) {}
    static void clearWave() {}
    static void runWave() {}
    static UUID[] getWaveList() { return new UUID[0]; }
    static int getWaveSize() { return 0; }
    static boolean isAddedToTheWave(UUID uuid) { return false; }
    static void warnPlayer(Player p, String reason) {}
    static void addPermission(Player p, Permission permission) {}
    static void sendClientSidedBlock(Player p, Location loc, Material m, byte b) {}
    static void destroyClientSidedBlock(Player p, Location loc) {}
    static void removeClientSidedBlocks(Player p) {}
    static boolean containsClientSidedBlock(Player p, Location loc) { return false; }
    static Material getClientSidedBlockMaterial(Player p, Location loc) { return null; }
    static byte getClientSidedBlockData(Player p, Location loc) { return 0; }
    static void disableVelocityProtection(Player p, int ticks) {}
    static String getConfiguredCheckName(HackType hackType) { return hackType.name(); }
    static void setConfiguredCheckName(HackType hackType, String name) {}
    static void setOnGround(Player p, int ticks) {}
    static int getMaxPunishmentViolation(HackType hackType) { return 100; }
    static int getMinPunishmentViolation(HackType hackType) { return 10; }
    static boolean mayPunishPlayer(Player p, HackType hackType) { return sacVL(p) > 10; }
    static boolean hasVerboseEnabled(Player p) { return false; }
    static boolean hasNotificationsEnabled(Player p) { return false; }
    static void setVerbose(Player p, boolean value) {}
    static void setNotifications(Player p, boolean value) {}
    static void setVerbose(Player p, boolean value, int frequency) {}
    static void setNotifications(Player p, int frequency) {}
    static int getViolationDivisor(Player p, HackType hackType) { return 1; }
    static boolean isCheckEnabled(String checkName) {
        try {
            for (HackType ht : HackType.values()) {
                if (ht.name().equalsIgnoreCase(checkName)) return isEnabled(ht);
            }
        } catch (Exception e) {}
        return true;
    }
    static String getPlayerViolationData(Player p) {
        var s = sp(p);
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        s.checkManager.allChecks.values().forEach(c -> {
            Check ch = (Check) c;
            if (ch.violations > 0)
                sb.append(ch.getCheckName()).append(":").append((int) ch.violations).append(",");
        });
        String result = sb.toString();
        return result.endsWith(",") ? result.substring(0, result.length() - 1) : result;
    }
    static Player[] getOnlineMonitoredPlayers() {
        try {
            var pdm = SacAPI.INSTANCE.getPlayerDataManager();
            if (pdm == null) return new Player[0];
            var entries = pdm.getEntries();
            return entries.stream()
                .map(sp -> org.bukkit.Bukkit.getPlayer(sp.uuid))
                .filter(p -> p != null)
                .toArray(Player[]::new);
        } catch (Exception e) { return new Player[0]; }
    }
}
