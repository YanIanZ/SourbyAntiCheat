package dev.yanianz.sourbyanticheat.platform.bukkit.gui.menu;

import dev.yanianz.sourbyanticheat.manager.ReportManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Date;
import java.util.List;

/** Lists pending player reports; click to teleport to target; clear-all button. */
public final class ReportsMenu extends SacMenu {

    @Override protected int size() { return 54; }

    @Override
    protected Component title() {
        return Component.text("📋 ", Menus.ACCENT)
            .append(Component.text("Reports", Menus.SOFT_WHITE, TextDecoration.BOLD));
    }

    @Override
    protected void render() {
        Menus.fillBorder(inventory);

        List<ReportManager.Report> reports = ReportManager.getAllReports();

        if (reports.isEmpty()) {
            ItemStack empty = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta em = empty.getItemMeta();
            em.displayName(Component.text("No pending reports", Menus.DARK_MUTED));
            empty.setItemMeta(em);
            inventory.setItem(22, empty);
        } else {
            for (int i = 0; i < Math.min(reports.size(), Menus.CONTENT_SLOTS.length); i++) {
                ReportManager.Report r = reports.get(i);
                ItemStack item = new ItemStack(Material.PAPER);
                ItemMeta im = item.getItemMeta();
                im.displayName(Component.text(r.reporterName(), Menus.HIGHLIGHT)
                    .append(Component.text(" → ", Menus.MUTED))
                    .append(Component.text(r.targetName(), Menus.DANGER)));
                im.lore(List.of(
                    Component.empty(),
                    Component.text("  Reason  ", Menus.MUTED).append(Component.text(r.reason(), Menus.SOFT_WHITE)),
                    Component.text("  Time  ", Menus.MUTED).append(Component.text(Menus.TIME_FMT.format(new Date(r.timestamp())), Menus.DARK_MUTED)),
                    Component.empty(),
                    Component.text("  ▸ Click to teleport to " + r.targetName(), Menus.ACCENT)
                ));
                item.setItemMeta(im);
                Menus.setPDC(item, Menus.KEY_TYPE, "report_target");
                Menus.setPDC(item, Menus.KEY_VALUE, r.targetName());
                inventory.setItem(Menus.CONTENT_SLOTS[i], item);
            }

            // Clear all button (slot 53)
            ItemStack clear = new ItemStack(Material.BARRIER);
            ItemMeta cm = clear.getItemMeta();
            cm.displayName(Component.text("⚠ ", Menus.DANGER).append(Component.text("Clear All Reports", Menus.DANGER)));
            clear.setItemMeta(cm);
            Menus.setPDC(clear, Menus.KEY_TYPE, "reports_clear_all");
            inventory.setItem(53, clear);
        }

        // Back button (slot 45)
        ItemStack back = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.displayName(Component.text("← ", Menus.MUTED).append(Component.text("Back to Panel", Menus.SOFT_WHITE)));
        back.setItemMeta(bm);
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
            case "report_target" -> {
                Player target = Menus.resolvePlayer(value);
                if (target != null) {
                    viewer.teleport(target.getLocation());
                    viewer.sendMessage(Component.text("Teleported to " + value, Menus.ACCENT));
                } else {
                    viewer.sendMessage(Component.text("Player " + value + " is offline.", Menus.DANGER));
                }
            }
            case "reports_clear_all" -> {
                var all = ReportManager.getAllReports();
                for (var r : all) ReportManager.clearReports(r.target());
                viewer.sendMessage(Component.text("All reports cleared.", Menus.ACCENT));
                refresh();
            }
            case "back" -> new HubMenu().open(viewer);
        }
    }
}
