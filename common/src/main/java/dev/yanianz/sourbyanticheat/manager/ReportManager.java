package dev.yanianz.sourbyanticheat.manager;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.events.packets.ProxyAlertMessenger;
import dev.yanianz.sourbyanticheat.utils.anticheat.MessageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class ReportManager {

    private static final Map<UUID, List<Report>> reports = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private static int cooldownSeconds = 60;
    private static boolean broadcastEnabled = true;
    private static boolean discordEnabled = false;
    private static boolean proxyEnabled = false;
    private static int maxAgeDays = 7;
    private static boolean cleanupStarted = false;

    public static void init() {
        try {
            var config = SacAPI.INSTANCE.getConfigManager().getConfig();
            cooldownSeconds = config.getIntElse("report.cooldown-seconds", 60);
            broadcastEnabled = config.getBooleanElse("report.broadcast-to-staff", true);
            discordEnabled   = config.getBooleanElse("report.discord-webhook", false);
            proxyEnabled     = config.getBooleanElse("report.proxy-broadcast", false);
            maxAgeDays       = config.getIntElse("report.max-age-days", 7);
        } catch (Exception ignored) {}
        if (!cleanupStarted) {
            cleanupStarted = true;
            startCleanupTask();
        }
    }

    public record Report(UUID reporter, String reporterName, UUID target, String targetName, String reason, long timestamp) {}

    public static ReportResult fileReport(UUID reporter, String reporterName, UUID target, String targetName, String reason) {
        Long last = cooldowns.get(reporter);
        long now = System.currentTimeMillis();
        if (last != null && now - last < cooldownSeconds * 1000L) {
            long remaining = ((last + cooldownSeconds * 1000L) - now) / 1000L;
            return new ReportResult(false, "Please wait " + remaining + "s before reporting again.");
        }
        cooldowns.put(reporter, now);

        Report report = new Report(reporter, reporterName, target, targetName, reason, now);
        reports.computeIfAbsent(target, k -> new ArrayList<>()).add(report);

        // Staff broadcast
        if (broadcastEnabled) broadcastToStaff(reporterName, targetName, reason);

        // Discord webhook
        if (discordEnabled) sendDiscordReport(reporterName, targetName, reason);

        // Proxy broadcast
        if (proxyEnabled) broadcastToProxy(reporterName, targetName, reason);

        return new ReportResult(true, "Report submitted for " + targetName + ".");
    }

    private static void broadcastToStaff(String reporter, String target, String reason) {
        try {
            var alertMgr = SacAPI.INSTANCE.getAlertManager();
            if (alertMgr == null) return;
            String msg = "SAC » " + reporter + " reported " + target + ": " + reason;
            alertMgr.sendVerbose(MessageUtil.miniMessage(msg), null);
        } catch (Exception ignored) {}
    }

    private static void sendDiscordReport(String reporter, String target, String reason) {
        try {
            var disc = SacAPI.INSTANCE.getDiscordManager();
            if (disc != null) disc.sendReportAlert(reporter, target, reason);
        } catch (Exception ignored) {}
    }

    private static void broadcastToProxy(String reporter, String target, String reason) {
        try {
            if (ProxyAlertMessenger.canSendAlerts()) {
                ProxyAlertMessenger.sendPluginMessage("SAC » " + reporter + " reported " + target + ": " + reason);
            }
        } catch (Exception ignored) {}
    }

    public static List<Report> getReports(UUID target) {
        return reports.getOrDefault(target, List.of());
    }

    public static List<Report> getAllReports() {
        List<Report> all = new ArrayList<>();
        for (List<Report> list : reports.values()) all.addAll(list);
        all.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        return all;
    }

    public static void clearReports(UUID target) {
        reports.remove(target);
    }

    private static void startCleanupTask() {
        var plugin = SacAPI.INSTANCE.getGrimPlugin();
        SacAPI.INSTANCE.getScheduler().getAsyncScheduler().runAtFixedRate(plugin, () -> {
            long cutoff = System.currentTimeMillis() - (maxAgeDays * 86400000L);
            reports.entrySet().removeIf(entry -> {
                entry.getValue().removeIf(r -> r.timestamp < cutoff);
                return entry.getValue().isEmpty();
            });
        }, 5, 5, TimeUnit.MINUTES);
    }

    public record ReportResult(boolean success, String message) {}

    private ReportManager() {}
}
