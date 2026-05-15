package dev.yanianz.sourbyanticheat.platform.bukkit.initables;

import dev.yanianz.sourbyanticheat.manager.init.start.StartableInitable;
import dev.yanianz.sourbyanticheat.platform.bukkit.SacBukkitLoaderPlugin;
import dev.yanianz.sourbyanticheat.utils.anticheat.Constants;
import io.github.retrooper.packetevents.bstats.bukkit.Metrics;

public class BukkitBStats implements StartableInitable {
    @Override
    public void start() {
        try {
            new Metrics(SacBukkitLoaderPlugin.LOADER, Constants.BSTATS_PLUGIN_ID);
        } catch (Exception ignored) {}
    }
}
