package dev.yanianz.sourbyanticheat.manager;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import org.bukkit.Bukkit;

public final class AutoPunishment {

    private static String command;
    private static int threshold;
    private static boolean enabled;

    public static void init() {
        try {
            var config = SacAPI.INSTANCE.getConfigManager().getConfig();
            command = config.getStringElse("punishment.command", "");
            threshold = config.getIntElse("punishment.threshold", 100);
            enabled = !command.isEmpty();
        } catch (Exception e) {
            enabled = false;
        }
    }

    public static void checkAndExecute(SacPlayer player, Check check) {
        if (!enabled || command.isEmpty()) return;

        int totalVL = (int) player.checkManager.allChecks.values().stream()
            .mapToDouble(c -> ((Check) c).violations).sum();

        if (totalVL >= threshold) {
            String cmd = command
                .replace("%player%", player.getName())
                .replace("%uuid%", player.uuid.toString())
                .replace("%check%", check.getCheckName())
                .replace("%vl%", String.valueOf(totalVL));

            LogUtil.info("Auto-punishment: " + cmd);
            Bukkit.getScheduler().runTask(
                (org.bukkit.plugin.Plugin) SacAPI.INSTANCE.getSacPlugin(),
                () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
            );
        }
    }

    private AutoPunishment() {}
}
