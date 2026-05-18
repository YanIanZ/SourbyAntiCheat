package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.BlockBreakCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockBreak;

@CheckData(name = "CrossFastBreak", configName = "crossfastbreak", decay = 0.02, setback = 10, stableKey = "cross.fastbreak")
public class CrossFastBreak extends Check implements BlockBreakCheck {

    private int breakCount;
    private long lastReset;
    private int buffer;

    private int breakThreshold = 8;
    private double nettyRateThreshold = 18.0;

    public CrossFastBreak(SacPlayer player) {
        super(player);
        lastReset = System.currentTimeMillis();
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.breakThreshold      = config.getIntElse(base + "break-threshold",       8);
        this.nettyRateThreshold  = config.getDoubleElse(base + "netty-rate-threshold", 18.0);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.compensatedEntities.self.isDead) return;

        long now = System.currentTimeMillis();
        if (now - lastReset > 1000) {
            breakCount = 0;
            lastReset = now;
        }
        breakCount++;

        if (breakCount < breakThreshold) {
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > nettyRateThreshold;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "FastBreak");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 3) {
            flagAndAlert(String.format("breaks=%d/s netty=%.1f/s spartan=%s",
                breakCount, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
            return;
        }
        reward();
    }
}
