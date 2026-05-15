package dev.yanianz.sourbyanticheat.platform.bukkit.scheduler.bukkit;

import ac.grim.grimac.api.plugin.GrimPlugin;
import dev.yanianz.sourbyanticheat.platform.api.scheduler.GlobalRegionScheduler;
import dev.yanianz.sourbyanticheat.platform.api.scheduler.TaskHandle;
import dev.yanianz.sourbyanticheat.platform.bukkit.SacBukkitLoaderPlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

public class BukkitGlobalRegionScheduler implements GlobalRegionScheduler {

    private final BukkitScheduler bukkitScheduler = Bukkit.getScheduler();

    @Override
    public void execute(@NotNull GrimPlugin plugin, @NotNull Runnable task) {
        bukkitScheduler.runTask(SacBukkitLoaderPlugin.LOADER, task);
    }

    @Override
    public TaskHandle run(@NotNull GrimPlugin plugin, @NotNull Runnable task) {
        return new BukkitTaskHandle(bukkitScheduler.runTask(SacBukkitLoaderPlugin.LOADER, task));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay) {
        return new BukkitTaskHandle(bukkitScheduler.runTaskLater(SacBukkitLoaderPlugin.LOADER, task, delay));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return new BukkitTaskHandle(bukkitScheduler.runTaskTimer(SacBukkitLoaderPlugin.LOADER, task, initialDelayTicks, periodTicks));
    }

    @Override
    public void cancel(@NotNull GrimPlugin plugin) {
        bukkitScheduler.cancelTasks(SacBukkitLoaderPlugin.LOADER);
    }
}
