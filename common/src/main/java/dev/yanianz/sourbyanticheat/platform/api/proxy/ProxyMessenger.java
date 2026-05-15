package dev.yanianz.sourbyanticheat.platform.api.proxy;

import java.util.UUID;

public interface ProxyMessenger {
    void sendAlert(UUID playerUuid, String alertJson);
    void broadcastMessage(byte[] data);
    void registerAlertHandler(AlertHandler handler);

    @FunctionalInterface
    interface AlertHandler {
        void onAlert(UUID playerUuid, String serverName, String alertJson);
    }
}
