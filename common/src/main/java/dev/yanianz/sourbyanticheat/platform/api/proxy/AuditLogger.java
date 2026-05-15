package dev.yanianz.sourbyanticheat.platform.api.proxy;

import java.util.UUID;

public interface AuditLogger {
    void logAction(UUID actorUuid, String actorName, String action, String target, String detail, boolean success);
    void logConfigChange(UUID actorUuid, String actorName, String configKey, String oldValue, String newValue);
}
