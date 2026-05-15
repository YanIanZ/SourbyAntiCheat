package dev.yanianz.sourbyanticheat.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;

public class SacNettyInjector {

    private static final String HANDLER_NAME = "sac_netty_handler";
    private static boolean injectionFailed = false;

    public static boolean inject(String playerName, Channel channel) {
        if (injectionFailed) return false;
        try {
            ChannelPipeline pipeline = channel.pipeline();
            if (pipeline.get(HANDLER_NAME) != null) {
                pipeline.remove(HANDLER_NAME);
            }
            pipeline.addAfter("packet_handler", HANDLER_NAME, new SacNettyChannelHandler(playerName));
            return true;
        } catch (Exception e) {
            injectionFailed = true;
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
}
