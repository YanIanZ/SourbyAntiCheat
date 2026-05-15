package dev.yanianz.sourbyanticheat.netty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.logging.Logger;

public class SacNettyChannelHandler extends ChannelDuplexHandler {

    private static final Logger LOGGER = Logger.getLogger("SAC-Netty");
    private static final int FLOOD_THRESHOLD = 500; // packets per second
    private static final long HIGH_DELAY_MS = 250;

    private final String playerName;
    private long packetCount = 0;
    private long floodResetTime = System.currentTimeMillis();
    private long lastReadTime = System.currentTimeMillis();
    private int floodWarnCount = 0;

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
                LOGGER.warning("[SAC-Netty] Packet flood: " + playerName + " (" + packetCount + " pkt/s)");
            }
        }

        long elapsed = now - lastReadTime;
        if (elapsed > HIGH_DELAY_MS && lastReadTime > 0) {
            LOGGER.fine("[SAC-Netty] High delay on " + playerName + ": " + elapsed + "ms");
        }
        lastReadTime = now;
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.fine("[SAC-Netty] Channel active: " + playerName);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.fine("[SAC-Netty] Channel closed: " + playerName + " | packets: " + packetCount);
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.warning("[SAC-Netty] Exception in " + playerName + ": " + cause.getMessage());
        ctx.close();
    }
}
