package dev.yanianz.sourbyanticheat.logging;

import com.google.gson.GsonBuilder;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ViolationEntry(
    Instant timestamp,
    String server,
    PlayerInfo player,
    CheckInfo check,
    ViolationInfo violation,
    SnapshotInfo snapshot
) {
    public record PlayerInfo(UUID uuid, String name, int clientVersion, boolean isBedrock, String proxyServer) {}
    public record CheckInfo(String type, String subtype, String confidence, List<String> source) {}
    public record ViolationInfo(int vl, int threshold, int ping, double tps) {}
    public record SnapshotInfo(double x, double y, double z, double dx, double dy, double dz,
                                String gamemode, boolean isFlying) {}

    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s/%s | VL=%d/%d | MS=%d",
            timestamp, player.name(), check.type(), check.subtype(),
            violation.vl(), violation.threshold(), violation.ping());
    }
}
