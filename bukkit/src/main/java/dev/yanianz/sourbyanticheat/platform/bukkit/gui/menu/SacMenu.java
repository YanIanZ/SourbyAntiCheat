package dev.yanianz.sourbyanticheat.platform.bukkit.gui.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/** Base for all SAC menus. The inventory's holder IS the menu, so click
 *  routing never parses titles. Subclasses carry their own state in fields. */
public abstract class SacMenu implements InventoryHolder {

    protected Inventory inventory;

    protected abstract int size();
    protected abstract Component title();
    protected abstract void render();

    /** Handle a click on a slot (already cancelled by the router). */
    public abstract void onClick(Player viewer, int slot, InventoryClickEvent event);

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            // Legacy-String title: a relocated Adventure Component would throw
            // NoSuchMethodError against Paper's createInventory(..,Component).
            inventory = Bukkit.createInventory(this, size(), Menus.legacy(title()));
            render();
        }
        return inventory;
    }

    public void open(Player player) {
        player.openInventory(getInventory());
    }

    /** Rebuild contents in place (after a state change) without reopening. */
    protected void refresh() {
        if (inventory != null) {
            inventory.clear();
            render();
        }
    }
}
