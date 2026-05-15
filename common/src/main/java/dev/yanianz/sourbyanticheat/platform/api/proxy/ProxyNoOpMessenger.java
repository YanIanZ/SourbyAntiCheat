package dev.yanianz.sourbyanticheat.platform.api.proxy;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ProxyNoOpMessenger implements ProxyMessenger, GlobalPlayerStore, AuditLogger {

    @Override
    public void sendAlert(UUID playerUuid, String alertJson) {}

    @Override
    public void broadcastMessage(byte[] data) {}

    @Override
    public void registerAlertHandler(AlertHandler handler) {}

    @Override
    public CompletableFuture<Integer> getViolations(UUID playerUuid, String checkType) {
        return CompletableFuture.completedFuture(0);
    }

    @Override
    public CompletableFuture<Void> setExempt(UUID playerUuid, boolean exempt, String reason) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> isExempt(UUID playerUuid) {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<String> getCurrentServer(UUID playerUuid) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void setCurrentServer(UUID playerUuid, String serverName) {}

    @Override
    public void logAction(UUID actorUuid, String actorName, String action, String target, String detail, boolean success) {}

    @Override
    public void logConfigChange(UUID actorUuid, String actorName, String configKey, String oldValue, String newValue) {}
}
