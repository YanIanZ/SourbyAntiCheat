package dev.yanianz.sourbyanticheat.manager.init.start;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.player.SacPlayer;

// Intended for future events we inject all platforms at the end of a tick
public abstract class AbstractTickEndEvent implements StartableInitable {

    @Override
    public void start() {

    }

    protected void onEndOfTick(SacPlayer player, boolean flush) {
        player.checkManager.getPacketEntityReplication().onEndOfTickEvent(true, flush);
    }

    protected boolean shouldInjectEndTick() {
        return SacAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("Reach.enable-post-packet", false);
    }
}
