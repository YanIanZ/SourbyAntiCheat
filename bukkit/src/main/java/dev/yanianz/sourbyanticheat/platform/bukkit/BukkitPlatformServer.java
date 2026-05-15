package dev.yanianz.sourbyanticheat.platform.bukkit;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.platform.api.Platform;
import dev.yanianz.sourbyanticheat.platform.api.PlatformServer;
import dev.yanianz.sourbyanticheat.platform.api.sender.Sender;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class BukkitPlatformServer implements PlatformServer {

    @Override
    public String getPlatformImplementationString() {
        return Bukkit.getVersion();
    }

    @Override
    public void dispatchCommand(Sender sender, String command) {
        CommandSender commandSender = SacBukkitLoaderPlugin.LOADER.getBukkitSenderFactory().reverse(sender);
        Bukkit.dispatchCommand(commandSender, command);
    }

    @Override
    public Sender getConsoleSender() {
        return SacBukkitLoaderPlugin.LOADER.getBukkitSenderFactory().map(Bukkit.getConsoleSender());
    }

    @Override
    public void registerOutgoingPluginChannel(String name) {
        SacBukkitLoaderPlugin.LOADER.getServer().getMessenger().registerOutgoingPluginChannel(SacBukkitLoaderPlugin.LOADER, name);
    }

    @Override
    public double getTPS() {
        // Folia throws UnsupportedOperationException on calling getTPS(), there is no API for getting TPS on Folia
        if (SacAPI.INSTANCE.getPlatform() == Platform.FOLIA) {
            return Double.NaN;
        }
        return SpigotReflectionUtil.getTPS();
    }
}
