package dev.yanianz.sourbyanticheat.checks.impl.movement;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "SafeWalk", stableKey = "sac.movement.safewalk", description = "Detects SafeWalk / edge walk hacks", setback = 5, decay = 0.02)
public class SafeWalk extends Check implements PacketCheck {

    private int stopTicks = 0;
    private double lastDeltaX, lastDeltaZ;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double deltaThreshold = 0.05;
    private double offsetThreshold = 0.001;
    private int tickWindow = 10;
    private int bufferThreshold = 2;

    public SafeWalk(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.deltaThreshold = config.getDoubleElse(base + "delta-threshold", 0.05);
        this.offsetThreshold = config.getDoubleElse(base + "offset-threshold", 0.001);
        this.tickWindow = config.getIntElse(base + "tick-window", 10);
        this.bufferThreshold = config.getIntElse(base + "buffer-threshold", 2);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.canFly || player.isFlying || player.isGliding || player.inVehicle()) return;

        double deltaX = Math.abs(player.x - player.lastX);
        double deltaZ = Math.abs(player.z - player.lastZ);

        // Knockback, hitting a wall or being shoved by a mob all legitimately stop
        // horizontal movement abruptly — exempt so legit sudden stops do not flag.
        boolean externalStop = player.likelyKB != null || player.firstBreadKB != null
                || player.horizontalCollision;
        if (externalStop) {
            stopTicks = Math.max(0, stopTicks - 1);
            if (stopTicks < bufferThreshold) reward();
            lastDeltaX = deltaX;
            lastDeltaZ = deltaZ;
            return;
        }

        // SafeWalk hard-stops movement at a ledge. Detect a sudden stop on EITHER axis —
        // a pure-Z bypass would otherwise go undetected.
        boolean suddenStopX = lastDeltaX > deltaThreshold && deltaX < offsetThreshold;
        boolean suddenStopZ = lastDeltaZ > deltaThreshold && deltaZ < offsetThreshold;

        if (suddenStopX || suddenStopZ) {
            stopTicks++;
            if (stopTicks > tickWindow) {
                flagAndAlert("sudden_stop ticks=" + stopTicks);
            }
        } else {
            stopTicks = Math.max(0, stopTicks - 1);
            if (stopTicks < bufferThreshold) reward();
        }

        lastDeltaX = deltaX;
        lastDeltaZ = deltaZ;
    }
}
