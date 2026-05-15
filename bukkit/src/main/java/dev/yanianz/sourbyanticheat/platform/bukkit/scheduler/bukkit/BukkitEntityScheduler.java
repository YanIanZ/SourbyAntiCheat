package dev.yanianz.sourbyanticheat.platform.bukkit.scheduler.bukkit;

import ac.grim.grimac.api.plugin.GrimPlugin;
import dev.yanianz.sourbyanticheat.platform.api.entity.SacEntity;
import dev.yanianz.sourbyanticheat.platform.api.scheduler.EntityScheduler;
import dev.yanianz.sourbyanticheat.platform.api.scheduler.TaskHandle;
import dev.yanianz.sourbyanticheat.platform.bukkit.SacBukkitLoaderPlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BukkitEntityScheduler implements EntityScheduler {
    private final BukkitScheduler scheduler = Bukkit.getScheduler();

    @Override
    public void execute(@NotNull SacEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable run, @Nullable Runnable retired, long delay) {
        scheduler.runTaskLater(SacBukkitLoaderPlugin.LOADER, run, delay);
    }

    @Override
    public TaskHandle run(@NotNull SacEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired) {
        return new BukkitTaskHandle(scheduler.runTask(SacBukkitLoaderPlugin.LOADER, task));
    }

    @Override
    public TaskHandle runDelayed(@NotNull SacEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long delayTicks) {
        return new BukkitTaskHandle(scheduler.runTaskLater(SacBukkitLoaderPlugin.LOADER, task, delayTicks));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull SacEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long initialDelayTicks, long periodTicks) {
        return new BukkitTaskHandle(scheduler.runTaskTimer(SacBukkitLoaderPlugin.LOADER, task, initialDelayTicks, periodTicks));
    }
}
