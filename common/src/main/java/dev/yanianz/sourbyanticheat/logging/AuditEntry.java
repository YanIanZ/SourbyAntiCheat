package dev.yanianz.sourbyanticheat.logging;

import com.google.gson.GsonBuilder;
import java.time.Instant;
import java.util.UUID;

public record AuditEntry(
    Instant timestamp,
    String actorName,
    UUID actorUuid,
    String action,
    String target,
    String detail,
    boolean success
) {
    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }

    @Override
    public String toString() {
        return String.format("[AUDIT %s] %s → %s | %s | %s → %s",
            timestamp, actorName, action, target,
            success ? "SUCCESS" : "FAILED", detail);
    }
}
