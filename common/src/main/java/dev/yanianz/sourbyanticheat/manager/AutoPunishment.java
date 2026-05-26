package dev.yanianz.sourbyanticheat.manager;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import dev.yanianz.sourbyanticheat.utils.anticheat.MessageUtil;
import org.bukkit.Bukkit;

public final class AutoPunishment {

    private static String banCommand;
    private static String kickCommand;
    private static String warnMessage;
    private static int banThreshold = 200;
    private static int kickThreshold = 150;
    private static int warnThreshold = 100;
    private static double maxVL = -1;
    private static boolean enabled;
    private static final java.util.Set<java.util.UUID> punishedPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static void init() {
        try {
            var config = SacAPI.INSTANCE.getConfigManager().getConfig();
            banCommand    = config.getStringElse("punishment.ban-command", "");
            kickCommand   = config.getStringElse("punishment.kick-command", "");
            warnMessage   = config.getStringElse("punishment.warn-message", "");
            banThreshold  = config.getIntElse("punishment.ban-threshold", 200);
            kickThreshold = config.getIntElse("punishment.kick-threshold", 150);
            warnThreshold = config.getIntElse("punishment.warn-threshold", 100);
            maxVL         = config.getDoubleElse("punishment.max-vl", -1);
            // Legacy VL-threshold punisher is OFF by default; punishments.yml is
            // the single pipeline. Staff alerts now come only from the [alert] route.
            boolean legacy = config.getBooleanElse("punishment.legacy-auto-enabled", false);
            boolean hasAction = !banCommand.isEmpty() || !kickCommand.isEmpty() || !warnMessage.isEmpty();
            enabled = legacy && hasAction;
        } catch (Exception e) {
            enabled = false;
        }
    }

    public static double getMaxVL() { return maxVL; }
    public static boolean isEnabled() { return enabled; }

    public static void checkAndExecute(SacPlayer player, Check check) {
        if (!enabled) return;
        if (punishedPlayers.contains(player.uuid)) return;

        int checkVL = (int) check.violations;
        int totalVL = (int) player.checkManager.allChecks.values().stream()
            .mapToDouble(c -> ((Check) c).violations).sum();

        // Per-check thresholds override globals; -1 = use global
        int banVl  = check.getPunishBanVL()  > 0 ? check.getPunishBanVL()  : banThreshold;
        int kickVl = check.getPunishKickVL() > 0 ? check.getPunishKickVL() : kickThreshold;
        int warnVl = check.getPunishWarnVL() > 0 ? check.getPunishWarnVL() : warnThreshold;

        boolean punished = false;
        if (checkVL >= banVl && !banCommand.isEmpty()) {
            executeCommand(banCommand, player, check, totalVL);
            WavePunishment.addToWave(player.uuid, player.getName(), check.getCheckName());
            punished = true;
        } else if (checkVL >= kickVl && !kickCommand.isEmpty()) {
            executeCommand(kickCommand, player, check, totalVL);
            punished = true;
        } else if (checkVL >= warnVl && !warnMessage.isEmpty()) {
            warnPlayer(player, check, totalVL);
        }

        if (punished) punishedPlayers.add(player.uuid);
    }

    private static void executeCommand(String cmd, SacPlayer player, Check check, int totalVL) {
        String formatted = cmd
            .replace("%player%", player.getName())
            .replace("%uuid%", player.uuid.toString())
            .replace("%check%", check.getCheckName())
            .replace("%vl%", String.valueOf(totalVL));

        LogUtil.info("Auto-punishment: " + formatted);
        var plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("SourbyAntiCheat");
        Bukkit.getScheduler().runTask(plugin,
            () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formatted));
    }

    private static void warnPlayer(SacPlayer player, Check check, int totalVL) {
        try {
            if (player.platformPlayer == null) return;
            String msg = warnMessage
                .replace("%player%", player.getName())
                .replace("%check%", check.getCheckName())
                .replace("%vl%", String.valueOf(totalVL));
            // Send through the platform abstraction — calling the native Bukkit
            // Player.sendMessage(Component) fails with NoSuchMethodError because our
            // adventure is relocated/shaded and Paper's API expects its own copy.
            player.platformPlayer.sendMessage(MessageUtil.miniMessage(msg));
        } catch (Exception ignored) {}
    }

    private AutoPunishment() {}
}
