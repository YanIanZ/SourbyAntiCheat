package dev.yanianz.sourbyanticheat.platform.bukkit.scheduler.folia;

import ac.grim.grimac.api.plugin.GrimPlugin;
import dev.yanianz.sourbyanticheat.platform.api.entity.SacEntity;
import dev.yanianz.sourbyanticheat.platform.api.scheduler.EntityScheduler;
import dev.yanianz.sourbyanticheat.platform.api.scheduler.TaskHandle;
import dev.yanianz.sourbyanticheat.platform.bukkit.SacBukkitLoaderPlugin;
import dev.yanianz.sourbyanticheat.platform.bukkit.entity.BukkitSacEntity;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FoliaEntityScheduler implements EntityScheduler {

    @Override
    public void execute(@NotNull SacEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long delay) {
        ((BukkitSacEntity) entity).getBukkitEntity().getScheduler().execute(SacBukkitLoaderPlugin.LOADER, task, retired, delay);
    }

    @Override
    public TaskHandle run(@NotNull SacEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired) {
        ScheduledTask scheduled = ((BukkitSacEntity) entity).getBukkitEntity().getScheduler().run(
                SacBukkitLoaderPlugin.LOADER,
                ignored -> task.run(),
                retired
        );

        return scheduled == null ? null : new FoliaTaskHandle(scheduled);
    }

    @Override
    public TaskHandle runDelayed(@NotNull SacEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long delayTicks) {
        ScheduledTask scheduled = ((BukkitSacEntity) entity).getBukkitEntity().getScheduler().runDelayed(
                SacBukkitLoaderPlugin.LOADER,
                ignored -> task.run(),
                retired,
                delayTicks
        );

        return scheduled == null ? null : new FoliaTaskHandle(scheduled);
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull SacEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long initialDelayTicks, long periodTicks) {
        ScheduledTask scheduled = ((BukkitSacEntity) entity).getBukkitEntity().getScheduler().runAtFixedRate(
                SacBukkitLoaderPlugin.LOADER,
                ignored -> task.run(),
                retired,
                initialDelayTicks,
                periodTicks
        );

        return scheduled == null ? null : new FoliaTaskHandle(scheduled);
    }
}
