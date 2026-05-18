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

import java.util.LinkedList;

@CheckData(name = "CrossAutoClicker", configName = "crossautoclicker", decay = 0.01, setback = 15, stableKey = "cross.autoclicker")
public class CrossAutoClicker extends Check implements PacketCheck {

    private final LinkedList<Long> clickTimes = new LinkedList<>();
    private int buffer;
    private static final int MAX_SAMPLES = 30;
    private double nettyVarianceThreshold = 10.0;
    private double nettyRateThreshold = 15.0;
    private int cpsThresholdLowPing = 18;
    private int cpsThresholdMedPing = 15;
    private int cpsThresholdHighPing = 12;
    private int cpsThresholdVeryHighPing = 10;

    public CrossAutoClicker(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        this.nettyVarianceThreshold = config.getDoubleElse(getConfigName() + ".netty-variance-threshold", 10.0);
        this.nettyRateThreshold = config.getDoubleElse(getConfigName() + ".netty-rate-threshold", 15.0);
        this.cpsThresholdLowPing = config.getIntElse(getConfigName() + ".cps-threshold-low-ping", 18);
        this.cpsThresholdMedPing = config.getIntElse(getConfigName() + ".cps-threshold-med-ping", 15);
        this.cpsThresholdHighPing = config.getIntElse(getConfigName() + ".cps-threshold-high-ping", 12);
        this.cpsThresholdVeryHighPing = config.getIntElse(getConfigName() + ".cps-threshold-very-high-ping", 10);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.compensatedEntities.self.isDead) return;

        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

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
        // Single prune via while-loop — no redundant removeIf
        while (!clickTimes.isEmpty() && now - clickTimes.getFirst() > 1_000_000_000L) {
            clickTimes.removeFirst();
        }

        int cps = clickTimes.size();
        int ping = player.getTransactionPing();

        if (cps < 8 || ping > 500) {
            reward();
            return;
        }

        int effectiveThreshold;
        if (ping < 50) effectiveThreshold = cpsThresholdLowPing;
        else if (ping < 150) effectiveThreshold = cpsThresholdMedPing;
        else if (ping < 300) effectiveThreshold = cpsThresholdHighPing;
        else effectiveThreshold = cpsThresholdVeryHighPing;

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
        // range = max - min (renamed from misleading 'variance')
        double range = n > 0 ? maxInterval - minInterval : 0;

        boolean consistentPattern = range < 25.0 && cps > 12;
        boolean highCPS = cps > effectiveThreshold + 5;

        if (!consistentPattern && !highCPS) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyIntervalVariance < nettyVarianceThreshold
            && player.crossValidationData.nettyPacketRatePerSec > nettyRateThreshold;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "AutoClicker");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            buffer += 2;
        } else {
            buffer += 1;
        }

        if (buffer > 5) {
            flagAndAlert(String.format("cps=%d ping=%dms range=%.1fms nettyVar=%.1f spartan=%s",
                cps, ping, range,
                player.crossValidationData.nettyIntervalVariance, spartanResult.type()));
        }
    }
}
