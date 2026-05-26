package dev.yanianz.sourbyanticheat.platform.bukkit.gui.menu;

import dev.yanianz.sourbyanticheat.manager.AlertFeed;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Date;
import java.util.List;

/** Live staff alert feed — newest first, click an entry to inspect that player. */
public final class AlertsMenu extends SacMenu {

    @Override protected int size() { return 54; }

    @Override
    protected Component title() {
        return Component.text("🔔 ", Menus.HIGHLIGHT)
            .append(Component.text("Alerts", Menus.SOFT_WHITE, TextDecoration.BOLD));
    }

    @Override
    protected void render() {
        Menus.fillBorder(inventory);

        List<AlertFeed.Entry> entries = AlertFeed.GLOBAL.recent();

        if (entries.isEmpty()) {
            ItemStack empty = Menus.item(Material.GRAY_STAINED_GLASS_PANE,
                Component.text("No recent alerts", Menus.DARK_MUTED),
                null);
            inventory.setItem(22, empty);
        } else {
            for (int i = 0; i < Math.min(entries.size(), Menus.CONTENT_SLOTS.length); i++) {
                AlertFeed.Entry entry = entries.get(i);
                ItemStack item = Menus.item(Material.PAPER,
                    Component.text(entry.player(), Menus.SOFT_WHITE, TextDecoration.BOLD)
                        .append(Component.text("  [" + entry.check() + "]", Menus.ACCENT)),
                    List.of(
                        Component.empty(),
                        Component.text("  VL  ", Menus.MUTED).append(Component.text(entry.vl(), Menus.vlColor(entry.vl()), TextDecoration.BOLD)),
                        Component.text("  Time  ", Menus.MUTED).append(Component.text(Menus.TIME_FMT.format(new Date(entry.ts())), Menus.DARK_MUTED)),
                        Component.text("  Info  ", Menus.MUTED).append(Component.text(entry.verbose(), Menus.MUTED)),
                        Component.empty(),
                        Component.text("  ▸ Click to inspect player", Menus.ACCENT)
                    ));
                Menus.setPDC(item, Menus.KEY_TYPE, "alert_player");
                Menus.setPDC(item, Menus.KEY_VALUE, entry.player());
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
        String type  = Menus.getPDC(item, Menus.KEY_TYPE);
        String value = Menus.getPDC(item, Menus.KEY_VALUE);
        if (type == null) return;

        switch (type) {
            case "alert_player" -> new PlayerChecksMenu(value).open(viewer);
            case "back"         -> new HubMenu().open(viewer);
        }
    }
}
