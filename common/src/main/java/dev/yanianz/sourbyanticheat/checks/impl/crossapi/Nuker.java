package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.BlockBreakCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockBreak;

import java.util.LinkedList;

@CheckData(name = "Nuker", configName = "nuker", decay = 0.02, setback = 15, stableKey = "cross.nuker")
public class Nuker extends Check implements BlockBreakCheck {

    private final LinkedList<Long> breakTimes = new LinkedList<>();
    private int buffer;
    private static final int MAX_BREAKS_PER_SEC = 15;
    private static final double NETTY_RATE_THRESHOLD = 18.0;

    public Nuker(SacPlayer player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.compensatedEntities.self.isDead) return;

        long now = System.nanoTime();
        breakTimes.add(now);
        while (!breakTimes.isEmpty() && now - breakTimes.getFirst() > 1_000_000_000L) {
            breakTimes.removeFirst();
        }

        int bps = breakTimes.size();

        if (bps < MAX_BREAKS_PER_SEC) {
            reward();
            return;
        }

        double avgInterval = 0;
        Long prev = null;
        for (long t : breakTimes) {
            if (prev != null) avgInterval += (t - prev) / 1_000_000.0;
            prev = t;
        }
        int n = breakTimes.size() - 1;
        if (n > 0) avgInterval /= n;

        boolean consistentRate = avgInterval < 70 && bps > 12;

        if (!consistentRate) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "FastBreak");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 4) {
            flagAndAlert(String.format("bps=%d int=%.1fms netty=%.1f/s spartan=%s",
                bps, avgInterval,
                player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
