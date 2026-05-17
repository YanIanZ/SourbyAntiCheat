package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "CrossSpeedB", configName = "crossspeedb", decay = 0.01, setback = 10, stableKey = "cross.speed_b")
public class CrossSpeedB extends Check implements PacketCheck {

    private double buffer;
    private double lastSpeed = 0;
    private double lastAccel = 0;
    private static final double ACCEL_LIMIT = 0.3;
    private static final double NETTY_RATE_THRESHOLD = 18.0;

    public CrossSpeedB(SacPlayer player) { super(player); }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.compensatedEntities.self.isDead) return;
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport || player.inVehicle()
                || player.canFly || player.isGliding
                || player.likelyKB != null || player.firstBreadKB != null) return;

        double dx = player.x - player.lastX;
        double dz = player.z - player.lastZ;
        double speed = Math.sqrt(dx * dx + dz * dz);
        if (speed < 0.01) { buffer = Math.max(0, buffer - 0.01); lastSpeed = speed; return; }

        double accel = Math.abs(speed - lastSpeed);
        double jerk = Math.abs(accel - lastAccel);
        boolean unnaturalJerk = jerk > ACCEL_LIMIT && speed > 0.15;

        lastSpeed = speed;
        lastAccel = accel;

        if (!unnaturalJerk) { buffer = Math.max(0, buffer - 0.01); reward(); return; }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "Speed");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 1.5 : 0.5;
        if (buffer > 2.5) {
            flagAndAlert(String.format("jerk=%.3f speed=%.2f netty=%.1f/s spartan=%s",
                jerk, speed, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
