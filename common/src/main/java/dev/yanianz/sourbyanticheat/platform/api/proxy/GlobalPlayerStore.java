package dev.yanianz.sourbyanticheat.platform.api.proxy;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface GlobalPlayerStore {
    CompletableFuture<Integer> getViolations(UUID playerUuid, String checkType);
    CompletableFuture<Void> setExempt(UUID playerUuid, boolean exempt, String reason);
    CompletableFuture<Boolean> isExempt(UUID playerUuid);
    CompletableFuture<String> getCurrentServer(UUID playerUuid);
    void setCurrentServer(UUID playerUuid, String serverName);
}
