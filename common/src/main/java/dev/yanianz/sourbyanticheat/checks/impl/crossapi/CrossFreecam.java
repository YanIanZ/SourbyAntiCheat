package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
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

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double offsetThreshold       = 0.8;
    private long   chunkGapThresholdMs   = 1500L;
    private double nettyRateThreshold    = 15.0;
    private double nettyDelayThreshold   = 50.0;

    public CrossFreecam(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        offsetThreshold     = config.getDoubleElse(base + "offset-threshold",        0.8);
        chunkGapThresholdMs = config.getIntElse(base + "chunk-gap-threshold-ms",     1500);
        nettyRateThreshold  = config.getDoubleElse(base + "netty-rate-threshold",    15.0);
        nettyDelayThreshold = config.getDoubleElse(base + "netty-delay-threshold",   50.0);
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
        boolean predictionFlag = offset > offsetThreshold && chunkGap > chunkGapThresholdMs;

        if (!predictionFlag) {
            buffer = Math.max(0, buffer - 0.05);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec < nettyRateThreshold
            || player.crossValidationData.nettyAvgDelayBetweenPacketsMs > nettyDelayThreshold;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Freecam");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            buffer += 1.5;
            if (buffer > 3.0) {
                flagAndAlert(String.format("offset=%.3f chunkGap=%dms netty=%.1f/s spartan=%s",
                    offset, chunkGap,
                    player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
                return;
            }
        } else {
            buffer += 0.5;
            if (buffer > 5.0) {
                flagAndAlert(String.format("offset=%.3f chunkGap=%dms (no cross-confirm)",
                    offset, chunkGap));
                return;
            }
        }
        // reward() only on clean paths — do NOT call here (suspicious tick)
    }
}
