package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

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
    private static final int PING_SAMPLE_SIZE = 10;

    public PingSpoof(SacPlayer player) {
        super(player);
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
        if (recentPings.size() > PING_SAMPLE_SIZE) recentPings.removeFirst();

        if (recentPings.size() < PING_SAMPLE_SIZE) return;

        double avgPing = recentPings.stream().mapToLong(Long::longValue).average().orElse(0);
        double minPing = recentPings.stream().mapToLong(Long::longValue).min().orElse(0);
        double maxPing = recentPings.stream().mapToLong(Long::longValue).max().orElse(0);
        double variance = (maxPing - minPing) / Math.max(avgPing, 1);

        int pingGap = Math.abs(transactionPing - keepAlivePing);
        boolean pingMismatch = pingGap > 200 && transactionPing > 300;
        boolean tooStable = variance < 0.05 && avgPing > 200;

        if (pingMismatch || tooStable) {
            stableHighPingCount++;
        } else {
            stableHighPingCount = Math.max(0, stableHighPingCount - 1);
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        if (stableHighPingCount < 5) return;

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec < 10.0;

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
