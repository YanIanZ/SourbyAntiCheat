package dev.yanianz.sourbyanticheat.platform.bukkit.gui.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Single listener: routes clicks to the SacMenu that owns the inventory. */
public final class MenuRouter implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SacMenu menu)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;
        menu.onClick(player, event.getRawSlot(), event);
    }
}
