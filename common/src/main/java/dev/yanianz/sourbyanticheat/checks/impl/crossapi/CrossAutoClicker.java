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

@CheckData(name = "CrossAutoClicker", configName = "crossautoclicker", decay = 0.01, setback = 15, stableKey = "cross.autoclicker")
public class CrossAutoClicker extends Check implements PacketCheck {

    private final LinkedList<Long> clickTimes = new LinkedList<>();
    private int buffer;
    private static final int MAX_SAMPLES = 30;
    private static final double NETTY_VARIANCE_THRESHOLD = 10.0;

    public CrossAutoClicker(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.compensatedEntities.self.isDead) return;

        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        boolean isAttack = false;
        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            isAttack = true;
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            isAttack = interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK;
        }

        if (!isAttack) return;

        long now = System.nanoTime();
        clickTimes.add(now);
        while (!clickTimes.isEmpty() && now - clickTimes.getFirst() > 1_000_000_000L) {
            clickTimes.removeFirst();
        }
        clickTimes.removeIf(t -> now - t > 2_000_000_000L);

        int cps = clickTimes.size();
        int ping = player.getTransactionPing();

        if (cps < 8 || ping > 500) {
            reward();
            return;
        }

        int effectiveThreshold;
        if (ping < 50) effectiveThreshold = 18;
        else if (ping < 150) effectiveThreshold = 15;
        else if (ping < 300) effectiveThreshold = 12;
        else effectiveThreshold = 10;

        if (cps < effectiveThreshold) {
            reward();
            return;
        }

        double avgInterval = 0;
        double minInterval = Double.MAX_VALUE;
        double maxInterval = 0;
        Long prev = null;
        for (long t : clickTimes) {
            if (prev != null) {
                double interval = (t - prev) / 1_000_000.0;
                avgInterval += interval;
                if (interval < minInterval) minInterval = interval;
                if (interval > maxInterval) maxInterval = interval;
            }
            prev = t;
        }
        int n = clickTimes.size() - 1;
        if (n > 0) avgInterval /= n;
        double variance = n > 0 ? maxInterval - minInterval : 0;

        boolean consistentPattern = variance < 25.0 && cps > 12;
        boolean highCPS = cps > effectiveThreshold + 5;

        if (!consistentPattern && !highCPS) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyIntervalVariance < NETTY_VARIANCE_THRESHOLD
            && player.crossValidationData.nettyPacketRatePerSec > 12.0;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "AutoClicker");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            buffer += 2;
        } else {
            buffer += 1;
        }

        if (buffer > 5) {
            flagAndAlert(String.format("cps=%d ping=%dms var=%.1fms nettyVar=%.1f spartan=%s",
                cps, ping, variance,
                player.crossValidationData.nettyIntervalVariance, spartanResult.type()));
        }
    }
}
