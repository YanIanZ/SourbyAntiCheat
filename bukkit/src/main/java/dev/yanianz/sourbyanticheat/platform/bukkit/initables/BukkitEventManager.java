package dev.yanianz.sourbyanticheat.platform.bukkit.initables;

import dev.yanianz.sourbyanticheat.manager.init.start.StartableInitable;
import dev.yanianz.sourbyanticheat.platform.bukkit.SacBukkitLoaderPlugin;
import dev.yanianz.sourbyanticheat.platform.bukkit.events.PistonEvent;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import org.bukkit.Bukkit;

public class BukkitEventManager implements StartableInitable {
    public void start() {
        LogUtil.info("Registering singular bukkit event... (PistonEvent)");

        Bukkit.getPluginManager().registerEvents(new PistonEvent(), SacBukkitLoaderPlugin.LOADER);
    }
}
