package dev.yanianz.sourbyanticheat.manager;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class AutoPunishment {

    private static String banCommand;
    private static String kickCommand;
    private static String warnMessage;
    private static int banThreshold = 200;
    private static int kickThreshold = 150;
    private static int warnThreshold = 100;
    private static boolean enabled;

    public static void init() {
        try {
            var config = SacAPI.INSTANCE.getConfigManager().getConfig();
            banCommand = config.getStringElse("punishment.ban-command", "");
            kickCommand = config.getStringElse("punishment.kick-command", "");
            warnMessage = config.getStringElse("punishment.warn-message", "");
            banThreshold = config.getIntElse("punishment.ban-threshold", 200);
            kickThreshold = config.getIntElse("punishment.kick-threshold", 150);
            warnThreshold = config.getIntElse("punishment.warn-threshold", 100);
            enabled = !banCommand.isEmpty() || !kickCommand.isEmpty() || !warnMessage.isEmpty();
        } catch (Exception e) {
            enabled = false;
        }
    }

    public static void checkAndExecute(SacPlayer player, Check check) {
        if (!enabled) return;

        int totalVL = (int) player.checkManager.allChecks.values().stream()
            .mapToDouble(c -> ((Check) c).violations).sum();

        if (totalVL >= banThreshold && !banCommand.isEmpty()) {
            executeCommand(banCommand, player, check, totalVL);
            WavePunishment.addToWave(player.uuid, player.getName(), check.getCheckName());
        } else if (totalVL >= kickThreshold && !kickCommand.isEmpty()) {
            executeCommand(kickCommand, player, check, totalVL);
        } else if (totalVL >= warnThreshold && !warnMessage.isEmpty()) {
            warnPlayer(player, check, totalVL);
        }
    }

    private static void executeCommand(String cmd, SacPlayer player, Check check, int totalVL) {
        String formatted = cmd
            .replace("%player%", player.getName())
            .replace("%uuid%", player.uuid.toString())
            .replace("%check%", check.getCheckName())
            .replace("%vl%", String.valueOf(totalVL));

        LogUtil.info("Auto-punishment executing: " + formatted);
        var plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("SourbyAntiCheat");
        Bukkit.getScheduler().runTask(plugin,
            () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formatted));
    }

    private static void warnPlayer(SacPlayer player, Check check, int totalVL) {
        try {
            if (player.platformPlayer == null) return;
            Player p = (Player) player.platformPlayer.getClass()
                .getMethod("getPlayer").invoke(player.platformPlayer);
            if (p == null) return;
            String msg = warnMessage
                .replace("%player%", player.getName())
                .replace("%check%", check.getCheckName())
                .replace("%vl%", String.valueOf(totalVL));
            p.sendMessage(net.kyori.adventure.text.Component.text(msg,
                net.kyori.adventure.text.format.NamedTextColor.RED));
        } catch (Exception ignored) {}
    }

    private AutoPunishment() {}
}
