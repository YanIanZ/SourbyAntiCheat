package dev.yanianz.sourbyanticheat.platform.api;

import ac.grim.grimac.api.plugin.GrimPlugin;
import dev.yanianz.sourbyanticheat.platform.api.command.CommandService;
import dev.yanianz.sourbyanticheat.platform.api.manager.ItemResetHandler;
import dev.yanianz.sourbyanticheat.platform.api.manager.MessagePlaceHolderManager;
import dev.yanianz.sourbyanticheat.platform.api.manager.PermissionRegistrationManager;
import dev.yanianz.sourbyanticheat.platform.api.manager.PlatformPluginManager;
import dev.yanianz.sourbyanticheat.platform.api.player.PlatformPlayerFactory;
import dev.yanianz.sourbyanticheat.platform.api.proxy.AuditLogger;
import dev.yanianz.sourbyanticheat.platform.api.proxy.GlobalPlayerStore;
import dev.yanianz.sourbyanticheat.platform.api.proxy.ProxyMessenger;
import dev.yanianz.sourbyanticheat.platform.api.proxy.ProxyNoOpMessenger;
import dev.yanianz.sourbyanticheat.platform.api.scheduler.PlatformScheduler;
import dev.yanianz.sourbyanticheat.platform.api.sender.SenderFactory;
import com.github.retrooper.packetevents.PacketEventsAPI;
import org.jetbrains.annotations.NotNull;

public interface PlatformLoader {
    PlatformScheduler getScheduler();

    PlatformPlayerFactory getPlatformPlayerFactory();

    PacketEventsAPI<?> getPacketEvents();

    ItemResetHandler getItemResetHandler();

    CommandService getCommandService();

    SenderFactory<?> getSenderFactory();

    GrimPlugin getPlugin();

    PlatformPluginManager getPluginManager();

    PlatformServer getPlatformServer();

    // Intended for use for platform specific service/API bringup
    // Method will be called when InitManager.load() is called
    void registerAPIService();

    // Used to replace text placeholders in messages
    // Currently only supports PlaceHolderAPI on Bukkit
    @NotNull
    MessagePlaceHolderManager getMessagePlaceHolderManager();

    PermissionRegistrationManager getPermissionManager();

    default ProxyMessenger getProxyMessenger() { return new ProxyNoOpMessenger(); }
    default GlobalPlayerStore getGlobalPlayerStore() { return (GlobalPlayerStore) getProxyMessenger(); }
    default AuditLogger getAuditLogger() { return (AuditLogger) getProxyMessenger(); }
}
