package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
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
    private double consistentRatio;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double maxRatioDeviation = 0.3;
    private double ratioThreshold    = 1.5;

    public CrossSpeedB(SacPlayer player) { super(player); }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        maxRatioDeviation = config.getDoubleElse(base + "max-ratio-deviation", 0.3);
        ratioThreshold    = config.getDoubleElse(base + "ratio-threshold", 1.5);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport || player.inVehicle()
                || player.canFly || player.isGliding
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR
                || player.compensatedEntities.self.isDead) return;

        double velX = player.clientVelocity.getX();
        double velZ = player.clientVelocity.getZ();
        double velH = Math.sqrt(velX * velX + velZ * velZ);
        if (velH < 0.01) return;

        double actualX = player.crossValidationData.pePositionDeltaX;
        double actualZ = player.crossValidationData.pePositionDeltaZ;
        double actualH = Math.sqrt(actualX * actualX + actualZ * actualZ);
        if (actualH < 0.01) return;

        double ratio = actualH / velH;
        double diff = Math.abs(ratio - consistentRatio);

        if (diff < maxRatioDeviation && ratio > ratioThreshold) {
            buffer += 0.5;
        } else {
            buffer = Math.max(0, buffer - 0.5);
            consistentRatio = ratio;
        }

        if (buffer <= 4.0) {
            reward();
            return;
        }

        // The actualH/velH ratio is a self-derived heuristic and is noisy on its own
        // (clientVelocity and pePositionDelta are not tick-synchronised). It must never
        // alert without a genuine independent cross-confirmation, otherwise a legitimate
        // player whose ratio sits steadily above the threshold is flagged relentlessly.
        // A standalone netty packet rate is not a reliable second signal, so the alert is
        // gated purely on Spartan agreeing this player is speeding.
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "Speed");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (spartanConfirms) {
            flagAndAlert(String.format("ratio=%.2f buffer=%.1f netty=%.1f/s spartan=%s",
                ratio, buffer, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        } else {
            // Buffer over threshold but no cross-confirmation — decay and reward
            // so VL does not stagnate while no independent source agrees.
            buffer = Math.max(0, buffer - 0.5);
            reward();
        }
    }
}
