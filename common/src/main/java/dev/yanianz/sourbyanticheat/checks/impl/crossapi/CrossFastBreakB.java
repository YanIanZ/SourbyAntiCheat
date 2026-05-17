package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

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
    private static final long CONSISTENCY_THRESHOLD_MS = 10;

    public CrossFastBreakB(SacPlayer player) { super(player); }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        long now = System.currentTimeMillis();
        if (lastBreakTime > 0) {
            long interval = now - lastBreakTime;
            if (interval > 20 && interval < 200) {
                consistentIntervals++;
            } else {
                consistentIntervals = Math.max(0, consistentIntervals - 1);
            }
        }
        lastBreakTime = now;

        if (consistentIntervals < 8) { buffer = Math.max(0, buffer - 1); reward(); return; }

        boolean nettyConfirms = player.crossValidationData.nettyIntervalVariance < CONSISTENCY_THRESHOLD_MS;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "FastBreak");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 3) {
            flagAndAlert(String.format("consistent=%d nettyVar=%.1f spartan=%s",
                consistentIntervals, player.crossValidationData.nettyIntervalVariance, spartanResult.type()));
        }
    }
}
