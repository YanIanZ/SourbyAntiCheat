package dev.yanianz.sourbyanticheat.manager.tick.impl;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.manager.tick.Tickable;
import dev.yanianz.sourbyanticheat.player.SacPlayer;

public class TickInventory implements Tickable {
    @Override
    public void tick() {
        for (SacPlayer player : SacAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            player.inventory.inventory.getInventoryStorage().tickWithBukkit();
        }
    }
}
