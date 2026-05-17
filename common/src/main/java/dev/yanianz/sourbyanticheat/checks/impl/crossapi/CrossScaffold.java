package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.BlockPlaceCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockPlace;

@CheckData(name = "CrossScaffold", configName = "crossscaffold", decay = 0.02, setback = 10, stableKey = "cross.scaffold")
public class CrossScaffold extends BlockPlaceCheck {

    private int placeCount;
    private long lastReset;
    private int buffer;
    private static final int PLACE_THRESHOLD = 5;
    private static final double NETTY_RATE_THRESHOLD = 18.0;

    public CrossScaffold(SacPlayer player) {
        super(player);
        lastReset = System.currentTimeMillis();
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        long now = System.currentTimeMillis();
        if (now - lastReset > 1000) {
            placeCount = 0;
            lastReset = now;
        }
        placeCount++;

        if (placeCount < PLACE_THRESHOLD) return;

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Scaffold");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 3) {
            flagAndAlert(String.format("places=%d/s netty=%.1f/s spartan=%s",
                placeCount, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
