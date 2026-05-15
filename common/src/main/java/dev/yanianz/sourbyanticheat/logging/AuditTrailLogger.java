package dev.yanianz.sourbyanticheat.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;

public class AuditTrailLogger {

    private final Path auditFile;
    private final boolean enabled;

    public AuditTrailLogger(Path dataFolder, boolean enabled) {
        this.enabled = enabled;
        this.auditFile = dataFolder.resolve("logs").resolve("audit.log");
        if (enabled) {
            try {
                Files.createDirectories(auditFile.getParent());
            } catch (IOException e) {
                throw new RuntimeException("Failed to create audit log directory", e);
            }
        }
    }

    public void log(String actorName, UUID actorUuid, String action, String target, String detail, boolean success) {
        if (!enabled) return;
        AuditEntry entry = new AuditEntry(Instant.now(), actorName, actorUuid, action, target, detail, success);
        try {
            Files.writeString(auditFile, entry.toJson() + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[SAC] Failed to write audit log: " + e.getMessage());
        }
    }

    public void logConfigChange(String actorName, UUID actorUuid, String configKey, String oldValue, String newValue) {
        log(actorName, actorUuid, "CONFIG_CHANGE", configKey, oldValue + " -> " + newValue, true);
    }
}
