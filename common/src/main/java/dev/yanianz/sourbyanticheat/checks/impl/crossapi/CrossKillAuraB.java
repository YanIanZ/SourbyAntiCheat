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

@CheckData(name = "CrossKillAuraB", configName = "crosskillaurab", decay = 0.01, setback = 15, stableKey = "cross.killaura_b")
public class CrossKillAuraB extends Check implements PacketCheck {

    private final LinkedList<Long> attackTimes = new LinkedList<>();
    private int buffer;

    // Track sustained high-yaw ticks to avoid flagging single legit strafe-while-attack moments
    private int highYawTicks = 0;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double varianceThreshold       = 30.0;
    private double rotYawThreshold         = 10.0;
    private int    attackCountThreshold    = 25;
    private double nettyVarianceThreshold  = 15.0;
    // Minimum sustained ticks of high rotYaw before it counts (FP guard for legit strafe-attack)
    private int    sustainedYawTicks       = 3;

    public CrossKillAuraB(SacPlayer player) { super(player); }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        varianceThreshold      = config.getDoubleElse(base + "variance-threshold",       30.0);
        rotYawThreshold        = config.getDoubleElse(base + "rot-yaw-threshold",        10.0);
        attackCountThreshold   = config.getIntElse(base + "attack-count-threshold",      25);
        nettyVarianceThreshold = config.getDoubleElse(base + "netty-variance-threshold", 15.0);
        sustainedYawTicks      = config.getIntElse(base + "sustained-yaw-ticks",         3);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.compensatedEntities.self.isDead) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

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

        // Track sustained high-yaw: require it to persist over N attack packets to discount
        // single legit strafe-while-attack events (one wide turn is normal, many consecutive is not).
        if (rotYaw > rotYawThreshold) {
            highYawTicks++;
        } else {
            highYawTicks = Math.max(0, highYawTicks - 1);
        }

        boolean sustainedHighYaw = highYawTicks >= sustainedYawTicks;
        boolean combo = variance < varianceThreshold && sustainedHighYaw && count > attackCountThreshold;

        if (!combo) { buffer = Math.max(0, buffer - 1); reward(); return; }

        boolean nettyConfirms = player.crossValidationData.nettyIntervalVariance < nettyVarianceThreshold;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "KillAura");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 4) {
            flagAndAlert(String.format("cps=%d var=%.1f rot=%.1f yawTicks=%d nettyVar=%.1f spartan=%s",
                count/2, variance, rotYaw, highYawTicks,
                player.crossValidationData.nettyIntervalVariance, spartanResult.type()));
            return;
        }
        // No reward() on suspicious attack ticks
    }
}
