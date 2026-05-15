package dev.yanianz.sourbyanticheat.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.logging.Logger;

public class SacNettyChannelHandler extends ChannelDuplexHandler {

    private static final Logger LOGGER = Logger.getLogger("SAC-Netty");
    private static final int FLOOD_THRESHOLD = 500;
    private static final long HIGH_DELAY_MS = 250;
    private static final int MAX_PACKET_SIZE = 2_097_152;

    private enum ProtocolState { HANDSHAKE, LOGIN, PLAY, UNKNOWN }

    private final String playerName;
    private long packetCount = 0;
    private long floodResetTime = System.currentTimeMillis();
    private long lastReadTime = System.currentTimeMillis();
    private long lastWriteTime = System.currentTimeMillis();
    private int floodWarnCount = 0;
    private long totalBytesRead = 0;
    private long totalBytesWritten = 0;
    private ProtocolState state = ProtocolState.UNKNOWN;

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
            detectProtocolState(buf);
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

    private void detectProtocolState(ByteBuf buf) {
        if (buf.readableBytes() < 1) return;
        int packetId = buf.getByte(buf.readerIndex());
        int secondByte = buf.readableBytes() > 1 ? buf.getByte(buf.readerIndex() + 1) : -1;

        if (packetId == 0x00 && secondByte == 0x00 && state == ProtocolState.UNKNOWN) {
            state = ProtocolState.HANDSHAKE;
        } else if (packetId == 0x00 && state == ProtocolState.HANDSHAKE) {
            state = ProtocolState.LOGIN;
        } else if (state == ProtocolState.LOGIN || state == ProtocolState.HANDSHAKE) {
            state = ProtocolState.PLAY;
        }
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
        LOGGER.warning("[SAC-Netty] Error: " + playerName + " " + cause.getMessage());
        ctx.close();
    }
}
