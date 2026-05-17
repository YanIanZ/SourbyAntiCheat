package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

import java.util.LinkedList;

@CheckData(name = "PingSpoof", configName = "pingspoof", decay = 0.01, setback = 10, stableKey = "cross.pingspoof")
public class PingSpoof extends Check implements PacketCheck {

    private final LinkedList<Long> recentPings = new LinkedList<>();
    private int buffer;
    private int stableHighPingCount;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private int pingSampleSize = 10;
    private double pingGapThreshold = 200.0;
    private double transactionPingThreshold = 300.0;
    private double varianceThreshold = 0.05;
    private double avgPingThreshold = 200.0;
    private static final double NETTY_RATE_THRESHOLD = 15.0;

    public PingSpoof(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        pingSampleSize           = config.getIntElse(base + "ping-sample-size", 10);
        pingGapThreshold         = config.getDoubleElse(base + "ping-gap-threshold", 200.0);
        transactionPingThreshold = config.getDoubleElse(base + "transaction-ping-threshold", 300.0);
        varianceThreshold        = config.getDoubleElse(base + "variance-threshold", 0.05);
        avgPingThreshold         = config.getDoubleElse(base + "avg-ping-threshold", 200.0);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        if (event.getPacketType() != PacketType.Play.Client.PONG) return;

        int transactionPing = player.getTransactionPing();
        int keepAlivePing = player.getKeepAlivePing();

        if (transactionPing < 50 || keepAlivePing < 50) {
            stableHighPingCount = 0;
            recentPings.clear();
            return;
        }

        recentPings.add((long) transactionPing);
        if (recentPings.size() > pingSampleSize) recentPings.removeFirst();

        if (recentPings.size() < pingSampleSize) return;

        double avgPing = recentPings.stream().mapToLong(Long::longValue).average().orElse(0);
        double minPing = recentPings.stream().mapToLong(Long::longValue).min().orElse(0);
        double maxPing = recentPings.stream().mapToLong(Long::longValue).max().orElse(0);
        double variance = (maxPing - minPing) / Math.max(avgPing, 1);

        int pingGap = Math.abs(transactionPing - keepAlivePing);
        boolean pingMismatch = pingGap > pingGapThreshold && transactionPing > transactionPingThreshold;
        // A genuinely stable fibre/LAN connection has consistent high transaction ping
        // AND a matching keep-alive ping. Spoofers fake one metric — require the gap
        // between the two pings to also be suspicious before treating stability as a signal.
        boolean tooStable = variance < varianceThreshold && avgPing > avgPingThreshold
            && pingGap > pingGapThreshold;

        if (pingMismatch || tooStable) {
            stableHighPingCount++;
        } else {
            stableHighPingCount = Math.max(0, stableHighPingCount - 1);
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        if (stableHighPingCount < 5) return;

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec < NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Exploits");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 4) {
            flagAndAlert(String.format("txPing=%d kaPing=%d gap=%d var=%.3f avg=%.0f spartan=%s",
                transactionPing, keepAlivePing, pingGap, variance, avgPing, spartanResult.type()));
        }
    }
}
