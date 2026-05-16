package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

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
    private static final double ROTATION_THRESHOLD = 30.0;
    private static final double ATTACK_INTERVAL_THRESHOLD = 200.0;
    private static final double NETTY_RATE_THRESHOLD = 15.0;
    private static final double NETTY_VARIANCE_THRESHOLD = 25.0;

    public CrossKillAura(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
            reward();
            return;
        }

        WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
        if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        double rotSnap = Math.abs(player.crossValidationData.peRotationDeltaYaw);
        double attackInterval = player.crossValidationData.peAttackIntervalMs;
        boolean fastSnap = rotSnap > ROTATION_THRESHOLD
            && attackInterval > 0
            && attackInterval < ATTACK_INTERVAL_THRESHOLD;

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
            flagAndAlertWithSetback(String.format("yaw=%.1f int=%.0fms netty=%.1f/s var=%.1f spartan=%s",
                rotSnap, attackInterval,
                player.crossValidationData.nettyPacketRatePerSec,
                player.crossValidationData.nettyIntervalVariance, spartanResult.type()));
        }
    }
}
