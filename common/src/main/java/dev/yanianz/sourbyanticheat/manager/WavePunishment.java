package dev.yanianz.sourbyanticheat.manager;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import org.bukkit.Bukkit;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Wave punishment system — queues bans/kicks and executes them
 * in batches to prevent cheat developers from identifying which
 * check triggered the punishment.
 */
public final class WavePunishment {

    private static final Queue<PunishEntry> queue = new ConcurrentLinkedQueue<>();
    private static String command;
    private static int interval;
    private static boolean enabled;

    public static void init() {
        try {
            var config = SacAPI.INSTANCE.getConfigManager().getConfig();
            command = config.getStringElse("punishment.wave-command", "");
            interval = config.getIntElse("punishment.wave-interval", 60);
            enabled = !command.isEmpty();
            if (enabled) startWave();
        } catch (Exception e) {
            enabled = false;
        }
    }

    public static void addToWave(UUID uuid, String playerName, String reason) {
        if (!enabled) return;
        queue.add(new PunishEntry(uuid, playerName, reason));
        LogUtil.info("Wave: " + playerName + " queued for " + reason + " (size=" + queue.size() + ")");
    }

    private static void startWave() {
        var plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("SourbyAntiCheat");
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (queue.isEmpty()) return;
            PunishEntry entry = queue.poll();
            if (entry == null) return;

            String cmd = command
                .replace("%player%", entry.playerName)
                .replace("%uuid%", entry.uuid.toString());
            LogUtil.info("Wave executing: " + cmd);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }, interval * 20L, interval * 20L);
    }

    private record PunishEntry(UUID uuid, String playerName, String reason) {}

    private WavePunishment() {}
}
