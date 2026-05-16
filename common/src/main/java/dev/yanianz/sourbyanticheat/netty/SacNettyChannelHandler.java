package dev.yanianz.sourbyanticheat.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.io.IOException;
import java.net.SocketException;
import java.util.logging.Logger;

public class SacNettyChannelHandler extends ChannelDuplexHandler {

    private static final Logger LOGGER = Logger.getLogger("SAC-Netty");
    private static final int FLOOD_THRESHOLD = 200;
    private static final long HIGH_DELAY_MS = 250;
    private static final int MAX_PACKET_SIZE = 2_097_152;

    private final String playerName;
    private long packetCount = 0;
    private long floodResetTime = System.currentTimeMillis();
    private long lastReadTime = System.currentTimeMillis();
    private long lastWriteTime = System.currentTimeMillis();
    private int floodWarnCount = 0;
    private long totalBytesRead = 0;
    private long totalBytesWritten = 0;

    public SacNettyChannelHandler(String playerName) {
        this.playerName = playerName;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        long now = System.currentTimeMillis();
        if (now - floodResetTime > 1000) {
            packetCount = 0;
            floodResetTime = now;
        }
        packetCount++;

        if (packetCount > FLOOD_THRESHOLD) {
            floodWarnCount++;
            if (floodWarnCount <= 3) {
                LOGGER.warning("[SAC-Netty] Flood: " + playerName + " " + packetCount + " pkt/s");
            }
        }

        if (msg instanceof ByteBuf buf) {
            int size = buf.readableBytes();
            totalBytesRead += size;
            if (size > MAX_PACKET_SIZE) {
                LOGGER.warning("[SAC-Netty] Oversized packet: " + playerName + " " + size + " bytes");
            }
        }

        long elapsed = now - lastReadTime;
        if (elapsed > HIGH_DELAY_MS && lastReadTime > 0) {
            LOGGER.fine("[SAC-Netty] Delay: " + playerName + " " + elapsed + "ms");
        }
        lastReadTime = now;
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
        LOGGER.fine("[SAC-Netty] Active: " + playerName);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.fine("[SAC-Netty] Closed: " + playerName
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
        LOGGER.warning("[SAC-Netty] Error: " + playerName + " " + cause.getMessage());
        ctx.close();
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