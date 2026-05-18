package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.BlockBreakCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockBreak;

@CheckData(name = "CrossFastBreakB", configName = "crossfastbreakb", decay = 0.02, setback = 10, stableKey = "cross.fastbreak_b")
public class CrossFastBreakB extends Check implements BlockBreakCheck {

    private int buffer;
    private long lastBreakTime = 0;
    private long consistentIntervals;

    // Gates variance (ms), not a raw timestamp — renamed from CONSISTENCY_THRESHOLD_MS
    private double intervalVarianceThreshold = 10.0;
    private long intervalMin = 20;
    private long intervalMax = 200;

    public CrossFastBreakB(SacPlayer player) { super(player); }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.intervalVarianceThreshold = config.getDoubleElse(base + "interval-variance-threshold", 10.0);
        this.intervalMin               = config.getIntElse(base + "interval-min-ms", 20);
        this.intervalMax               = config.getIntElse(base + "interval-max-ms", 200);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.compensatedEntities.self.isDead) return;

        long now = System.currentTimeMillis();
        if (lastBreakTime > 0) {
            long interval = now - lastBreakTime;
            if (interval > intervalMin && interval < intervalMax) {
                consistentIntervals++;
            } else {
                consistentIntervals = Math.max(0, consistentIntervals - 1);
            }
        }
        lastBreakTime = now;

        if (consistentIntervals < 8) { buffer = Math.max(0, buffer - 1); reward(); return; }

        boolean nettyConfirms = player.crossValidationData.nettyIntervalVariance < intervalVarianceThreshold;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "FastBreak");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 3) {
            flagAndAlert(String.format("consistent=%d nettyVar=%.1f spartan=%s",
                consistentIntervals, player.crossValidationData.nettyIntervalVariance, spartanResult.type()));
        }
    }
}
