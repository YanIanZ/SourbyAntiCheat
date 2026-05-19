package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "MorePackets", configName = "morepackets", decay = 0.01, setback = 10, stableKey = "cross.morepackets")
public class MorePackets extends Check implements PacketCheck {

    private int buffer;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double rateThreshold = 30.0;
    private double bytesThreshold = 1024.0;
    private double nettyRateThreshold = 120.0;

    public MorePackets(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        rateThreshold      = config.getDoubleElse(base + "rate-threshold", 30.0);
        bytesThreshold     = config.getDoubleElse(base + "bytes-threshold", 1024.0);
        nettyRateThreshold = config.getDoubleElse(base + "netty-rate-threshold", 120.0);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        double rate = player.crossValidationData.nettyPacketRatePerSec;
        double avgBytes = player.crossValidationData.nettyAvgReadBytesPerPacket;

        boolean rateFlag = rate > rateThreshold;
        boolean bytesFlag = avgBytes > bytesThreshold && rate > 20.0;

        if (!rateFlag && !bytesFlag) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > nettyRateThreshold;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "MorePackets");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        // Severe burst (rate > 40) only escalates fast when an independent signal
        // (netty rate / Spartan) confirms — otherwise treat it as a normal increment.
        if ((rate > 40.0 && nettyConfirms) || spartanConfirms) {
            buffer += 2;
        } else {
            buffer += 1;
        }

        if (buffer > 5) {
            flagAndAlert(String.format("rate=%.1f/s bytes=%.1f netty=%b spartan=%s",
                rate, avgBytes, nettyConfirms, spartanResult.type()));
        }
    }
}
