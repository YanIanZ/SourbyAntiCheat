package dev.yanianz.sourbyanticheat.netty;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.checks.crossapi.CrossValidationData;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.io.IOException;
import java.net.SocketException;
import java.util.UUID;
import java.util.logging.Logger;

public class SacNettyChannelHandler extends ChannelDuplexHandler {

    private static final Logger LOGGER = Logger.getLogger("SAC-Netty");
    private static final int FLOOD_THRESHOLD = 200;
    private static final long HIGH_DELAY_MS = 250;
    private static final int MAX_PACKET_SIZE = 2_097_152;

    private final UUID playerUuid;
    private final String playerName;
    private long packetCount = 0;
    private long floodResetTime = System.currentTimeMillis();
    private long lastReadTime = System.currentTimeMillis();
    private long lastWriteTime = System.currentTimeMillis();
    private int floodWarnCount = 0;
    private long totalBytesRead = 0;
    private long totalBytesWritten = 0;
    private long lastIntervalMs = -1;
    private long intervalVarianceAccum = 0;
    private int intervalSampleCount = 0;
    // Rate of the last fully-completed ~1s window. Reported as nettyPacketRatePerSec so
    // the value is stable — computing packetCount/elapsed mid-window gives wild spikes
    // right after a reset (tiny elapsed) and is what produced unusable rate readings.
    private double lastWindowPacketRate = 0;
    private long windowBytesRead = 0;
    private double lastWindowBytesPerPacket = 0;

    public SacNettyChannelHandler(UUID playerUuid, String playerName) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        long now = System.currentTimeMillis();
        // Roll the 1-second window: snapshot the completed window's rate before reset.
        long windowElapsed = now - floodResetTime;
        if (windowElapsed >= 1000) {
            lastWindowPacketRate = packetCount * 1000.0 / windowElapsed;
            lastWindowBytesPerPacket = packetCount > 0 ? (double) windowBytesRead / packetCount : 0;
            packetCount = 0;
            windowBytesRead = 0;
            floodResetTime = now;
        }
        packetCount++;

        if (lastIntervalMs >= 0) {
            long interval = now - lastReadTime;
            long delta = Math.abs(interval - lastIntervalMs);
            intervalVarianceAccum += delta;
            intervalSampleCount++;
        }
        lastIntervalMs = now - lastReadTime;

        if (packetCount > FLOOD_THRESHOLD) {
            floodWarnCount++;
            if (floodWarnCount <= 3) {
                LOGGER.warning("Flood: " + playerName + " " + packetCount + " pkt/s");
            }
        }

        if (msg instanceof ByteBuf buf) {
            int size = buf.readableBytes();
            totalBytesRead += size;
            windowBytesRead += size;
            if (size > MAX_PACKET_SIZE) {
                LOGGER.warning("Oversized packet: " + playerName + " " + size + " bytes");
            }
        }

        long elapsed = now - lastReadTime;
        if (elapsed > HIGH_DELAY_MS && lastReadTime > 0) {
            LOGGER.fine("Delay: " + playerName + " " + elapsed + "ms");
        }
        lastReadTime = now;

        updateCrossValidationData(now);

        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf buf) {
            totalBytesWritten += buf.readableBytes();
        }
        lastWriteTime = System.currentTimeMillis();
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.fine("Active: " + playerName);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.fine("Closed: " + playerName
            + " | rx=" + (totalBytesRead / 1024) + "KB"
            + " tx=" + (totalBytesWritten / 1024) + "KB"
            + " pkts=" + packetCount);
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException || cause instanceof SocketException
                || cause instanceof java.nio.channels.ClosedChannelException) {
            return;
        }
        LOGGER.warning("Error: " + playerName + " " + cause.getMessage());
        ctx.close();
    }

    private void updateCrossValidationData(long now) {
        try {
            var pdm = SacAPI.INSTANCE.getPlayerDataManager();
            if (pdm == null) return;
            SacPlayer sp = pdm.getPlayer(playerUuid);
            if (sp == null) return;
            CrossValidationData data = sp.crossValidationData;
            // Report the last completed 1-second window — stable, no mid-window spikes.
            data.nettyPacketRatePerSec = lastWindowPacketRate;
            data.nettyAvgReadBytesPerPacket = lastWindowBytesPerPacket;
            data.nettyAvgDelayBetweenPacketsMs = lastWindowPacketRate > 0
                ? 1000.0 / lastWindowPacketRate
                : 0;
            data.nettyIntervalVariance = intervalSampleCount > 0
                ? (double) intervalVarianceAccum / intervalSampleCount
                : 0;
        } catch (Exception ignored) {}
    }

    public double getPacketRatePerSecond() {
        long elapsed = System.currentTimeMillis() - floodResetTime;
        if (elapsed <= 0) return 0;
        return (packetCount * 1000.0) / elapsed;
    }

    public double getAvgReadBytesPerPacket() {
        return packetCount > 0 ? (double) totalBytesRead / packetCount : 0;
    }

    public double getAvgDelayBetweenPacketsMs() {
        return packetCount > 1 ? (double) (System.currentTimeMillis() - floodResetTime) / (packetCount - 1) : 0;
    }
}
