package dev.yanianz.sourbyanticheat.platform.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.yanianz.sourbyanticheat.platform.api.proxy.AuditLogger;
import dev.yanianz.sourbyanticheat.platform.api.proxy.GlobalPlayerStore;
import dev.yanianz.sourbyanticheat.platform.api.proxy.ProxyMessenger;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(
    id = "sourbyanticheat",
    name = "SourbyAntiCheat",
    version = "1.0.0",
    authors = {"YanIanZ"}
)
public class SacVelocityPlugin implements ProxyMessenger, GlobalPlayerStore, AuditLogger {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDir;
    private final Map<UUID, String> playerServers = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> violationCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> exemptPlayers = new ConcurrentHashMap<>();

    @Inject
    public SacVelocityPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDir) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDir = dataDir;
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        logger.info("SAC Velocity companion enabled");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("SAC Velocity companion disabled");
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        playerServers.put(event.getPlayer().getUniqueId(),
                event.getServer().getServerInfo().getName());
    }

    // ProxyMessenger
    @Override
    public void sendAlert(UUID playerUuid, String alertJson) {
        proxy.getAllPlayers().stream()
            .filter(p -> p.hasPermission("sac.alerts"))
            .forEach(p -> p.sendMessage(net.kyori.adventure.text.Component.text(alertJson)));
    }

    @Override
    public void broadcastMessage(byte[] data) {}

    @Override
    public void registerAlertHandler(AlertHandler handler) {}

    // GlobalPlayerStore
    @Override
    public CompletableFuture<Integer> getViolations(UUID playerUuid, String checkType) {
        return CompletableFuture.completedFuture(violationCounts.getOrDefault(playerUuid, 0));
    }

    @Override
    public CompletableFuture<Void> setExempt(UUID playerUuid, boolean exempt, String reason) {
        if (exempt) exemptPlayers.put(playerUuid, true);
        else exemptPlayers.remove(playerUuid);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> isExempt(UUID playerUuid) {
        return CompletableFuture.completedFuture(exemptPlayers.getOrDefault(playerUuid, false));
    }

    @Override
    public CompletableFuture<String> getCurrentServer(UUID playerUuid) {
        return CompletableFuture.completedFuture(playerServers.get(playerUuid));
    }

    @Override
    public void setCurrentServer(UUID playerUuid, String serverName) {
        playerServers.put(playerUuid, serverName);
    }

    // AuditLogger
    @Override
    public void logAction(UUID actorUuid, String actorName, String action, String target, String detail, boolean success) {
        logger.info("[AUDIT] {} → {} | {} | {} → {}", actorName, action, target, success ? "SUCCESS" : "FAILED", detail);
    }

    @Override
    public void logConfigChange(UUID actorUuid, String actorName, String configKey, String oldValue, String newValue) {
        logger.info("[AUDIT-CONFIG] {} → {}: {} → {}", actorName, configKey, oldValue, newValue);
    }

    public ProxyServer getProxy() { return proxy; }
    public Logger getLogger() { return logger; }
    public Path getDataDir() { return dataDir; }
}
