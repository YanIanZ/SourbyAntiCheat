package dev.yanianz.sourbyanticheat.checks.impl.aim;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.RotationCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.RotationUpdate;

import java.util.LinkedList;

@CheckData(name = "AimAssist", stableKey = "sac.aim.assist", description = "Detects aim assist via rotation analysis", setback = 5, decay = 0.01)
public class AimAssist extends Check implements RotationCheck {

    private static final int SAMPLE_SIZE = 40;
    private static final double SNAP_THRESHOLD = 30.0;
    private static final double SMOOTH_VARIANCE_MAX = 0.08;

    private final LinkedList<Float> deltaYaws = new LinkedList<>();
    private final LinkedList<Float> deltaPitches = new LinkedList<>();
    private boolean lastWasSnap = false;
    private int snapStreak = 0;
    private int ticksSinceSnapFlag = 0;

    public AimAssist(SacPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (player.packetStateData.lastPacketWasTeleport || player.vehicleData.wasVehicleSwitch) return;
        if (rotationUpdate.isCinematic()) return;
        if (player.inVehicle() || player.compensatedEntities.self.isDead
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        float deltaYaw = rotationUpdate.getDeltaXRotABS();
        float deltaPitch = rotationUpdate.getDeltaYRotABS();

        if (deltaYaw < 0.01f && deltaPitch < 0.01f) return;

        deltaYaws.add(deltaYaw);
        deltaPitches.add(deltaPitch);

        if (deltaYaws.size() > SAMPLE_SIZE) {
            deltaYaws.removeFirst();
            deltaPitches.removeFirst();
        }

        if (ticksSinceSnapFlag > 0) {
            ticksSinceSnapFlag--;
            return;
        }

        if (deltaYaws.size() >= SAMPLE_SIZE) {
            double yawVariance = calculateVariance(deltaYaws);
            double pitchVariance = calculateVariance(deltaPitches);

            // Snap detection: sudden large yaw changes on consecutive attack ticks
            if (deltaYaw > SNAP_THRESHOLD && lastWasSnap) {
                snapStreak++;
                if (snapStreak >= 8) {
                    flagAndAlert("snap=" + String.format("%.1f", deltaYaw) + " streak=" + snapStreak);
                    ticksSinceSnapFlag = 20;
                    snapStreak = 0;
                }
            } else if (deltaYaw > SNAP_THRESHOLD) {
                lastWasSnap = true;
                snapStreak = 1;
            } else {
                lastWasSnap = false;
                snapStreak = 0;
            }

            // Smooth detection: very consistent rotation suggesting algorithmic smoothing
            if (yawVariance < SMOOTH_VARIANCE_MAX && pitchVariance < SMOOTH_VARIANCE_MAX
                    && deltaYaw > 1.5f && deltaYaw < 8.0f) {
                flagAndAlert("smooth yawVar=" + String.format("%.4f", yawVariance) + " pitchVar=" + String.format("%.4f", pitchVariance));
                ticksSinceSnapFlag = 20;
            } else {
                reward();
            }
        }
    }

    private static double calculateVariance(LinkedList<Float> values) {
        double sum = 0;
        double sumSq = 0;
        int n = values.size();
        for (float v : values) {
            sum += v;
            sumSq += v * v;
        }
        double mean = sum / n;
        return (sumSq / n) - (mean * mean);
    }
}
