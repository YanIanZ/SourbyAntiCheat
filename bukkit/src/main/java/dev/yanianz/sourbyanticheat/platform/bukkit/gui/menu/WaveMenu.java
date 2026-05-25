package dev.yanianz.sourbyanticheat.platform.bukkit.gui.menu;

import dev.yanianz.sourbyanticheat.manager.WavePunishment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Shows the real wave-punishment queue; one item per queued entry. */
public final class WaveMenu extends SacMenu {

    @Override protected int size() { return 54; }

    @Override
    protected Component title() {
        return Component.text("⏳ ", Menus.ACCENT2)
            .append(Component.text("Wave Queue", Menus.SOFT_WHITE, TextDecoration.BOLD));
    }

    @Override
    protected void render() {
        Menus.fillBorder(inventory);

        List<String> queue = WavePunishment.queueView();

        if (queue.isEmpty()) {
            // Explanatory info item shown when queue is empty
            ItemStack info = Menus.item(Material.CLOCK,
                Component.text("Ban Wave System", Menus.HIGHLIGHT, TextDecoration.BOLD),
                List.of(
                    Component.empty(),
                    Component.text("  Bans are batched and executed", Menus.MUTED),
                    Component.text("  at intervals to prevent cheat", Menus.MUTED),
                    Component.text("  developers from identifying which", Menus.MUTED),
                    Component.text("  check triggered the punishment.", Menus.MUTED),
                    Component.empty(),
                    Component.text("  Queue is currently empty.", Menus.SUCCESS),
                    Component.empty(),
                    Component.text("  Config: punishment.wave-command", Menus.DARK_MUTED),
                    Component.text("  Config: punishment.wave-interval", Menus.DARK_MUTED)
                ));
            inventory.setItem(22, info);
        } else {
            for (int i = 0; i < Math.min(queue.size(), Menus.CONTENT_SLOTS.length); i++) {
                String entry = queue.get(i);
                ItemStack item = Menus.item(Material.PAPER,
                    Component.text(entry, Menus.SOFT_WHITE),
                    List.of(
                        Component.empty(),
                        Component.text("  Queued for wave execution", Menus.MUTED)
                    ));
                Menus.setPDC(item, Menus.KEY_TYPE, "wave_entry");
                inventory.setItem(Menus.CONTENT_SLOTS[i], item);
            }
        }

        // Back button (slot 45)
        ItemStack back = Menus.item(Material.SPECTRAL_ARROW,
            Component.text("← ", Menus.MUTED).append(Component.text("Back to Panel", Menus.SOFT_WHITE)),
            null);
        Menus.setPDC(back, Menus.KEY_TYPE, "back");
        inventory.setItem(45, back);
    }

    @Override
    public void onClick(Player viewer, int slot, InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        String type = Menus.getPDC(item, Menus.KEY_TYPE);
        if (type == null) return;

        if ("back".equals(type)) {
            new HubMenu().open(viewer);
        }
        // wave_entry items are view-only
    }
}
