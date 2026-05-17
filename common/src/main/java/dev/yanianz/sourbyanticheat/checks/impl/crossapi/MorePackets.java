package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "MorePackets", configName = "morepackets", decay = 0.01, setback = 10, stableKey = "cross.morepackets")
public class MorePackets extends Check implements PacketCheck {

    private int buffer;
    private static final double RATE_THRESHOLD = 30.0;
    private static final double BYTES_THRESHOLD = 1024.0;
    private static final double NETTY_RATE_THRESHOLD = 15.0;

    public MorePackets(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        double rate = player.crossValidationData.nettyPacketRatePerSec;
        double avgBytes = player.crossValidationData.nettyAvgReadBytesPerPacket;

        boolean rateFlag = rate > RATE_THRESHOLD;
        boolean bytesFlag = avgBytes > BYTES_THRESHOLD && rate > 20.0;

        if (!rateFlag && !bytesFlag) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "MorePackets");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (rate > 40.0 || spartanConfirms) {
            buffer += 2;
        } else {
            buffer += 1;
        }

        if (buffer > 5) {
            flagAndAlert(String.format("rate=%.1f/s bytes=%.1f spartan=%s",
                rate, avgBytes, spartanResult.type()));
        }
    }
}
