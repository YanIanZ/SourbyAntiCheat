package dev.yanianz.sourbyanticheat.platform.bukkit.scheduler.folia;

import ac.grim.grimac.api.plugin.GrimPlugin;
import dev.yanianz.sourbyanticheat.platform.api.scheduler.GlobalRegionScheduler;
import dev.yanianz.sourbyanticheat.platform.api.scheduler.TaskHandle;
import dev.yanianz.sourbyanticheat.platform.bukkit.SacBukkitLoaderPlugin;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class FoliaGlobalRegionScheduler implements GlobalRegionScheduler {

    private final io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler globalRegionScheduler = Bukkit.getGlobalRegionScheduler();

    @Override
    public void execute(@NotNull GrimPlugin plugin, @NotNull Runnable task) {
        globalRegionScheduler.execute(SacBukkitLoaderPlugin.LOADER, task);
    }

    @Override
    public TaskHandle run(@NotNull GrimPlugin plugin, @NotNull Runnable task) {
        return new FoliaTaskHandle(globalRegionScheduler.run(SacBukkitLoaderPlugin.LOADER, ignored -> task.run()));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay) {
        return new FoliaTaskHandle(globalRegionScheduler.runDelayed(SacBukkitLoaderPlugin.LOADER, ignored -> task.run(), delay));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return new FoliaTaskHandle(globalRegionScheduler.runAtFixedRate(SacBukkitLoaderPlugin.LOADER, ignored -> task.run(), initialDelayTicks, periodTicks));
    }

    @Override
    public void cancel(@NotNull GrimPlugin plugin) {
        globalRegionScheduler.cancelTasks(SacBukkitLoaderPlugin.LOADER);
    }
}
