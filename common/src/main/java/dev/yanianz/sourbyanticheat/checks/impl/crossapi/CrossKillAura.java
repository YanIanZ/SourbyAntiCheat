package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "CrossKillAura", configName = "crosskillaura", decay = 0.01, setback = 50, stableKey = "cross.killaura")
public class CrossKillAura extends Check implements PacketCheck {

    private int auraBuffer;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double rotationThreshold       = 30.0;
    private double attackIntervalThreshold = 200.0;
    private static final double NETTY_RATE_THRESHOLD     = 120.0;  // physics constant
    private static final double NETTY_VARIANCE_THRESHOLD = 25.0;  // physics constant

    public CrossKillAura(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        rotationThreshold       = config.getDoubleElse(base + "rotation-threshold",       30.0);
        attackIntervalThreshold = config.getDoubleElse(base + "attack-interval-threshold", 200.0);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.compensatedEntities.self.isDead) return;

        // Only process INTERACT_ENTITY (attack action). Non-attack packets do NOT trigger reward() —
        // that was the bug: calling reward() on 20+/sec movement packets massively suppressed VL.
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

        WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
        if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        double rotSnap = Math.abs(player.crossValidationData.peRotationDeltaYaw);
        double attackInterval = player.crossValidationData.peAttackIntervalMs;
        boolean fastSnap = rotSnap > rotationThreshold
            && attackInterval > 0
            && attackInterval < attackIntervalThreshold;

        if (!fastSnap) {
            auraBuffer = Math.max(0, auraBuffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD
            && player.crossValidationData.nettyIntervalVariance < NETTY_VARIANCE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "KillAura");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            auraBuffer += 2;
        } else {
            auraBuffer += 1;
        }

        if (auraBuffer > 5) {
            flagAndAlert(String.format("yaw=%.1f int=%.0fms netty=%.1f/s var=%.1f spartan=%s",
                rotSnap, attackInterval,
                player.crossValidationData.nettyPacketRatePerSec,
                player.crossValidationData.nettyIntervalVariance, spartanResult.type()));
            return;
        }
        // No reward() on suspicious attack ticks
    }
}
