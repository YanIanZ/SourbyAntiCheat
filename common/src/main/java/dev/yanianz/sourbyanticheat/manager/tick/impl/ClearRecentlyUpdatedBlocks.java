package dev.yanianz.sourbyanticheat.manager.tick.impl;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.manager.tick.Tickable;
import dev.yanianz.sourbyanticheat.player.SacPlayer;

public class ClearRecentlyUpdatedBlocks implements Tickable {

    private static final int maxTickAge = 2;

    @Override
    public void tick() {
        for (SacPlayer player : SacAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            player.blockHistory.cleanup(SacAPI.INSTANCE.getTickManager().currentTick - maxTickAge);
        }
    }
}
