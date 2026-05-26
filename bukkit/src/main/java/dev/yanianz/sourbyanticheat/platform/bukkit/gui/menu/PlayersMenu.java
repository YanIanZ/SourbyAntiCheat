package dev.yanianz.sourbyanticheat.platform.bukkit.gui.menu;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Paginated player list — shows player heads sorted by name. */
public final class PlayersMenu extends SacMenu {

    private final int page;

    public PlayersMenu(int page) {
        this.page = page;
    }

    @Override protected int size() { return 54; }

    @Override
    protected Component title() {
        return Component.text("✦ ", Menus.ACCENT)
            .append(Component.text("Players", Menus.SOFT_WHITE, TextDecoration.BOLD))
            .append(Component.text(" — Page " + (page + 1), Menus.MUTED));
    }

    @Override
    protected void render() {
        Menus.fillBorder(inventory);

        var pdm = SacAPI.INSTANCE.getPlayerDataManager();
        var entries = new ArrayList<>(pdm.getEntries());
        entries.sort(Comparator.comparing(SacPlayer::getName, String.CASE_INSENSITIVE_ORDER));

        int perPage = Menus.CONTENT_SLOTS.length; // 28
        int totalPages = (entries.size() + perPage - 1) / perPage;
        int start = page * perPage;

        for (int i = 0; i < perPage; i++) {
            int idx = start + i;
            if (idx >= entries.size()) break;

            SacPlayer sp = entries.get(idx);
            int totalVL = Menus.getTotalVL(sp);

            long activeChecks = sp.checkManager.allChecks.values().stream()
                .filter(c -> ((Check) c).violations > 0.5).count();

            ItemStack head = Menus.head(sp.uuid,
                Component.text("✦ ", Menus.ACCENT)
                    .append(Component.text(sp.getName(), Menus.SOFT_WHITE, TextDecoration.BOLD)),
                List.of(
                    Component.empty(),
                    Component.text("  VL  ", Menus.MUTED).append(Component.text(totalVL, Menus.vlColor(totalVL), TextDecoration.BOLD)),
                    Component.text("  Active Flags  ", Menus.MUTED).append(Component.text(activeChecks, activeChecks > 0 ? Menus.HIGHLIGHT : Menus.DARK_MUTED)),
                    Component.text("  Ping  ", Menus.MUTED).append(Component.text(sp.getTransactionPing() + "ms", Menus.DARK_MUTED)),
                    Component.empty(),
                    Component.text("  ▸ Click to inspect", Menus.ACCENT)
                ));
            Menus.setPDC(head, Menus.KEY_TYPE, "player");
            Menus.setPDC(head, Menus.KEY_VALUE, sp.getName());
            inventory.setItem(Menus.CONTENT_SLOTS[i], head);
        }

        // Back button (slot 45)
        ItemStack back = Menus.item(Material.SPECTRAL_ARROW,
            Component.text("← ", Menus.MUTED).append(Component.text("Back to Panel", Menus.SOFT_WHITE)),
            null);
        Menus.setPDC(back, Menus.KEY_TYPE, "back");
        inventory.setItem(45, back);

        // Prev page (slot 47)
        if (page > 0) {
            ItemStack prev = Menus.item(Material.ARROW,
                Component.text("◀ Previous Page", Menus.ACCENT),
                null);
            Menus.setPDC(prev, Menus.KEY_TYPE, "page_prev");
            inventory.setItem(47, prev);
        }

        // Next page (slot 51)
        if (page + 1 < totalPages) {
            ItemStack next = Menus.item(Material.ARROW,
                Component.text("Next Page ▶", Menus.ACCENT),
                null);
            Menus.setPDC(next, Menus.KEY_TYPE, "page_next");
            inventory.setItem(51, next);
        }
    }

    @Override
    public void onClick(Player viewer, int slot, InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        String type  = Menus.getPDC(item, Menus.KEY_TYPE);
        String value = Menus.getPDC(item, Menus.KEY_VALUE);
        if (type == null) return;

        switch (type) {
            case "player"    -> new PlayerChecksMenu(value).open(viewer);
            case "back"      -> new HubMenu().open(viewer);
            case "page_prev" -> new PlayersMenu(page - 1).open(viewer);
            case "page_next" -> new PlayersMenu(page + 1).open(viewer);
        }
    }
}
