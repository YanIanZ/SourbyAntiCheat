package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

@CheckData(name = "CrossFreecam", configName = "crossfreecam", decay = 0.05, setback = 15, stableKey = "cross.freecam")
public class CrossFreecam extends Check implements PostPredictionCheck {

    private double buffer;
    private long lastChunkAck = System.currentTimeMillis();
    private static final double OFFSET_THRESHOLD = 1.5;
    private static final long CHUNK_GAP_THRESHOLD_MS = 2000;
    private static final double NETTY_RATE_THRESHOLD = 15.0;
    private static final double NETTY_DELAY_THRESHOLD = 50.0;

    public CrossFreecam(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CHUNK_BATCH_ACK) {
            lastChunkAck = System.currentTimeMillis();
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.inVehicle() || player.compensatedEntities.self.isDead) return;

        double offset = player.crossValidationData.offsetFromPrediction;
        long chunkGap = System.currentTimeMillis() - lastChunkAck;
        boolean predictionFlag = offset > OFFSET_THRESHOLD && chunkGap > CHUNK_GAP_THRESHOLD_MS;

        if (!predictionFlag) {
            buffer = Math.max(0, buffer - 0.05);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec < NETTY_RATE_THRESHOLD
            || player.crossValidationData.nettyAvgDelayBetweenPacketsMs > NETTY_DELAY_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Freecam");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            buffer += 1.5;
            if (buffer > 3.0) {
                flagAndAlert(String.format("offset=%.3f chunkGap=%dms netty=%.1f/s spartan=%s",
                    offset, chunkGap,
                    player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
            }
        } else {
            buffer += 0.5;
            if (buffer > 5.0) {
                flagAndAlert(String.format("offset=%.3f chunkGap=%dms (no cross-confirm)",
                    offset, chunkGap));
            }
        }

        reward();
    }
}
