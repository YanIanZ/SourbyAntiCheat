package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "CrossTimer", configName = "crosstimer", decay = 0.01, setback = 50, stableKey = "cross.timer")
public class CrossTimer extends Check implements PacketCheck {

    private double balance;
    private static final double BALANCE_LIMIT = 20.0;
    private static final double NETTY_RATE_THRESHOLD = 22.0;

    public CrossTimer(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (!isUpdate(event.getPacketType())) return;

        balance += 1.0;
        balance -= 1.0;

        if (balance > BALANCE_LIMIT) balance = BALANCE_LIMIT;

        boolean balanceFlag = balance > 10.0;

        if (!balanceFlag) {
            if (balance < -5.0) {
                reward();
            }
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD
            || player.crossValidationData.nettyAvgDelayBetweenPacketsMs < 45.0;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Timer");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        String verbose = String.format("balance=%.1f netty=%.1f/s spartan=%s",
            balance,
            player.crossValidationData.nettyPacketRatePerSec,
            spartanResult.type());

        if (nettyConfirms || spartanConfirms) {
            flagAndAlertWithSetback(verbose);
        } else {
            flagAndAlert(verbose);
        }
    }
}
