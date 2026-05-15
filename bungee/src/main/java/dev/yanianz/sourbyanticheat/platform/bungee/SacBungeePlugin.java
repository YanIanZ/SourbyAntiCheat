package dev.yanianz.sourbyanticheat.platform.bungee;

import dev.yanianz.sourbyanticheat.platform.api.proxy.ProxyMessenger;
import dev.yanianz.sourbyanticheat.platform.api.proxy.GlobalPlayerStore;
import dev.yanianz.sourbyanticheat.platform.api.proxy.AuditLogger;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SacBungeePlugin extends Plugin implements ProxyMessenger, GlobalPlayerStore, AuditLogger {

    private final Map<UUID, String> playerServers = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> violationCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> exemptPlayers = new ConcurrentHashMap<>();
    private AlertHandler alertHandler;

    @Override
    public void onEnable() {
        getProxy().registerChannel("sac:main");
        getProxy().getPluginManager().registerListener(this, new BungeeProxyListener(this));
        getLogger().info("SAC BungeeCord companion enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("SAC BungeeCord companion disabled");
    }

    @Override
    public void sendAlert(UUID playerUuid, String alertJson) {
        try {
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(alertJson).getAsJsonObject();
            String player = json.has("player") ? json.get("player").getAsString() : "?";
            String check = json.has("check") ? json.get("check").getAsString() : "?";
            getLogger().info("[SAC] " + player + " flagged by " + check);

            for (var p : getProxy().getPlayers()) {
                if (p.hasPermission("sac.alerts")) {
                    p.sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacy(
                        "§c[SAC] §7" + player + " §eflagged by §6" + check));
                }
            }
        } catch (Exception ignored) {}

        if (alertHandler != null) {
            alertHandler.onAlert(playerUuid, "proxy", alertJson);
        }
    }

    @Override
    public void broadcastMessage(byte[] data) {
        for (var server : getProxy().getServers().values()) {
            server.sendData("sac:main", data, false);
        }
    }

    @Override
    public void registerAlertHandler(AlertHandler handler) {
        this.alertHandler = handler;
    }

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

    @Override
    public void logAction(UUID actorUuid, String actorName, String action, String target, String detail, boolean success) {
        getLogger().info(String.format("[AUDIT] %s → %s | %s | %s → %s",
                actorName, action, target, success ? "SUCCESS" : "FAILED", detail));
    }

    @Override
    public void logConfigChange(UUID actorUuid, String actorName, String configKey, String oldValue, String newValue) {
        getLogger().info(String.format("[AUDIT-CONFIG] %s → %s: %s → %s",
                actorName, configKey, oldValue, newValue));
    }

    public Map<UUID, Integer> getViolationCounts() { return violationCounts; }
}

class BungeeProxyListener implements net.md_5.bungee.api.plugin.Listener {
    private final SacBungeePlugin plugin;

    BungeeProxyListener(SacBungeePlugin plugin) { this.plugin = plugin; }

    @net.md_5.bungee.event.EventHandler
    public void onServerConnected(net.md_5.bungee.api.event.ServerConnectedEvent event) {
        plugin.setCurrentServer(event.getPlayer().getUniqueId(),
                event.getServer().getInfo().getName());
    }

    @net.md_5.bungee.event.EventHandler
    public void onPluginMessage(net.md_5.bungee.api.event.PluginMessageEvent event) {
        if (!event.getTag().equals("sac:main")) return;
        com.google.common.io.ByteArrayDataInput in =
                com.google.common.io.ByteStreams.newDataInput(event.getData());
        String subChannel = in.readUTF();
        if ("Alert".equals(subChannel)) {
            UUID playerUuid = UUID.fromString(in.readUTF());
            String serverName = in.readUTF();
            String alertJson = in.readUTF();
            plugin.sendAlert(playerUuid, alertJson);
        }
    }
}
