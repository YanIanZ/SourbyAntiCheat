package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "IrregularMovements", configName = "irregularmovements", decay = 0.02, setback = 10, stableKey = "cross.irregularmovements")
public class IrregularMovements extends Check implements PacketCheck {

    private int buffer;
    private double lastAccelX, lastAccelZ;
    private double lastSpeed;
    private int patternFlags;
    private static final double ACCEL_THRESHOLD = 0.5;
    private static final double SPEED_BURST_THRESHOLD = 0.8;
    private static final double NETTY_RATE_THRESHOLD = 20.0;

    public IrregularMovements(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        if (player.packetStateData.lastPacketWasTeleport
                || player.inVehicle() || player.canFly || player.isGliding
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE) {
            patternFlags = 0;
            return;
        }

        double deltaX = player.x - player.lastX;
        double deltaZ = player.z - player.lastZ;
        double speed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (speed < 0.01) return;

        double accelX = deltaX - lastAccelX;
        double accelZ = deltaZ - lastAccelZ;
        double accelChange = Math.sqrt(accelX * accelX + accelZ * accelZ);

        boolean suddenAccel = accelChange > ACCEL_THRESHOLD && lastSpeed > 0.05;
        boolean speedBurst = speed > SPEED_BURST_THRESHOLD && lastSpeed < 0.2;
        boolean pattern = suddenAccel || speedBurst;

        lastAccelX = deltaX;
        lastAccelZ = deltaZ;
        lastSpeed = speed;

        if (pattern) {
            patternFlags++;
        } else {
            patternFlags = Math.max(0, patternFlags - 1);
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        if (patternFlags < 2) return;

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "IrregularMovements");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            buffer += 2;
        } else {
            buffer += 1;
        }

        if (buffer > 4) {
            flagAndAlert(String.format("accel=%.2f speed=%.2f netty=%.1f/s spartan=%s",
                accelChange, speed,
                player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
