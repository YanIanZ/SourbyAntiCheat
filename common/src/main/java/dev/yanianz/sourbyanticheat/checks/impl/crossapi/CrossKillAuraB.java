package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

import java.util.LinkedList;

@CheckData(name = "CrossKillAuraB", configName = "crosskillaurab", decay = 0.01, setback = 15, stableKey = "cross.killaura_b")
public class CrossKillAuraB extends Check implements PacketCheck {

    private final LinkedList<Long> attackTimes = new LinkedList<>();
    private int buffer;

    public CrossKillAuraB(SacPlayer player) { super(player); }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        boolean isAttack = false;
        if (event.getPacketType() == PacketType.Play.Client.ATTACK) isAttack = true;
        else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            isAttack = interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK;
        }
        if (!isAttack) return;

        long now = System.nanoTime();
        attackTimes.add(now);
        while (!attackTimes.isEmpty() && now - attackTimes.getFirst() > 2_000_000_000L) attackTimes.removeFirst();

        int count = attackTimes.size();
        if (count < 20) { reward(); return; }

        double avgInterval = 0;
        double minInterval = Double.MAX_VALUE;
        double maxInterval = 0;
        Long prev = null;
        for (long t : attackTimes) {
            if (prev != null) {
                double interval = (t - prev) / 1_000_000.0;
                avgInterval += interval;
                if (interval < minInterval) minInterval = interval;
                if (interval > maxInterval) maxInterval = interval;
            }
            prev = t;
        }
        int n = attackTimes.size() - 1;
        if (n > 0) avgInterval /= n;
        double variance = n > 0 ? maxInterval - minInterval : 100;

        double rotYaw = Math.abs(player.crossValidationData.peRotationDeltaYaw);
        boolean combo = variance < 30.0 && rotYaw > 10.0 && count > 25;

        if (!combo) { buffer = Math.max(0, buffer - 1); reward(); return; }

        boolean nettyConfirms = player.crossValidationData.nettyIntervalVariance < 15.0;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "KillAura");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 4) {
            flagAndAlert(String.format("cps=%d var=%.1f rot=%.1f nettyVar=%.1f spartan=%s",
                count/2, variance, rotYaw, player.crossValidationData.nettyIntervalVariance, spartanResult.type()));
        }
    }
}
