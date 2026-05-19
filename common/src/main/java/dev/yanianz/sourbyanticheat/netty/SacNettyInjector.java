package dev.yanianz.sourbyanticheat.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;

import java.util.UUID;

public class SacNettyInjector {

    private static final String HANDLER_NAME = "sac_netty_handler";
    private static boolean injectionFailed = false;
    private static int successCount = 0;
    private static int failCount = 0;

    public static boolean inject(UUID playerUuid, String playerName, Channel channel) {
        if (injectionFailed) return false;
        try {
            ChannelPipeline pipeline = channel.pipeline();
            if (pipeline.get(HANDLER_NAME) != null) {
                pipeline.remove(HANDLER_NAME);
            }
            SacNettyChannelHandler handler = new SacNettyChannelHandler(playerUuid, playerName);
            // Insert before the vanilla packet decoder so channelRead sees the raw inbound
            // ByteBuf of each packet (accurate packet count AND byte size). Fall back to
            // a position-agnostic insert if the "packet_handler" anchor is absent.
            if (pipeline.get("packet_handler") != null) {
                pipeline.addBefore("packet_handler", HANDLER_NAME, handler);
            } else {
                pipeline.addLast(HANDLER_NAME, handler);
            }
            successCount++;
            return true;
        } catch (NoClassDefFoundError e) {
            injectionFailed = true;
            return false;
        } catch (Exception e) {
            failCount++;
            return false;
        }
    }

    public static void remove(Channel channel) {
        try {
            ChannelPipeline pipeline = channel.pipeline();
            if (pipeline.get(HANDLER_NAME) != null) {
                pipeline.remove(HANDLER_NAME);
            }
        } catch (Exception ignored) {}
    }

    public static boolean isInjectionFailed() { return injectionFailed; }
    public static void markInjectionFailed() { injectionFailed = true; }
    public static int getSuccessCount() { return successCount; }
    public static int getFailCount() { return failCount; }
}
