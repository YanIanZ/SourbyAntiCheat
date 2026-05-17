package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "CrossTimer", configName = "crosstimer", decay = 0.01, setback = 50, stableKey = "cross.timer")
public class CrossTimer extends Check implements PacketCheck {

    private double balance;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double balanceLimit        = 15.0;
    private double balanceFlagLimit    = 8.0;
    private double nettyRateThreshold  = 15.0;

    public CrossTimer(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        balanceLimit       = config.getDoubleElse(base + "balance-limit",        15.0);
        balanceFlagLimit   = config.getDoubleElse(base + "balance-flag-limit",   8.0);
        nettyRateThreshold = config.getDoubleElse(base + "netty-rate-threshold", 15.0);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        if (!isUpdate(event.getPacketType())) return;

        int ping = player.getTransactionPing();
        double multiplier = ping > 400 ? 0.5 : 1.0;

        balance += 1.0 * multiplier;
        balance -= 1.0;

        if (balance > balanceLimit) balance = balanceLimit;

        boolean balanceFlag = balance > balanceFlagLimit;

        if (!balanceFlag) {
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > nettyRateThreshold
            || player.crossValidationData.nettyAvgDelayBetweenPacketsMs < 50.0;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Timer");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        String verbose = String.format("balance=%.1f netty=%.1f/s spartan=%s crossConfirmed=%s",
            balance,
            player.crossValidationData.nettyPacketRatePerSec,
            spartanResult.type(),
            nettyConfirms || spartanConfirms);

        flagAndAlert(verbose);
    }
}
