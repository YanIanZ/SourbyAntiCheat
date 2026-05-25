package dev.yanianz.sourbyanticheat.platform.bukkit.gui.menu;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Per-player check detail panel — shows VL, enable state; click to toggle. */
public final class PlayerChecksMenu extends SacMenu {

    private final String targetName;

    public PlayerChecksMenu(String targetName) {
        this.targetName = targetName;
    }

    @Override protected int size() { return 54; }

    @Override
    protected Component title() {
        return Component.text("✦ ", Menus.ACCENT)
            .append(Component.text(targetName, Menus.SOFT_WHITE, TextDecoration.BOLD))
            .append(Component.text(" — Checks", Menus.MUTED));
    }

    @Override
    protected void render() {
        Menus.fillBorder(inventory);

        SacPlayer target = Menus.resolveSacPlayer(targetName);
        if (target == null) return;

        var checks = new ArrayList<>(target.checkManager.allChecks.entrySet());
        checks.sort(Comparator.comparing(e -> {
            Check c = (Check) e.getValue();
            return c.getCheckName() == null ? "" : c.getCheckName();
        }, String.CASE_INSENSITIVE_ORDER));

        int slot = 0;
        for (var entry : checks) {
            Check check = (Check) entry.getValue();
            if (check.getCheckName() == null) continue;
            if (slot >= Menus.CONTENT_SLOTS.length) break;

            double vl      = check.violations;
            boolean enabled = check.isEnabled();
            boolean experimental = check.isExperimental();

            Material mat;
            if (!enabled)     mat = Material.GRAY_STAINED_GLASS_PANE;
            else if (vl > 50) mat = Material.RED_CONCRETE;
            else if (vl > 10) mat = Material.ORANGE_CONCRETE;
            else if (vl > 1)  mat = Material.YELLOW_CONCRETE;
            else              mat = Material.LIME_CONCRETE;

            Component nameComp = Component.text(check.getCheckName(), enabled ? Menus.SOFT_WHITE : Menus.DARK_MUTED);
            if (experimental) nameComp = nameComp.append(Component.text(" *", Menus.PURPLE));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("  VL  ", Menus.MUTED)
                .append(Component.text(String.valueOf((int) vl), Menus.vlColor(vl), TextDecoration.BOLD)));
            lore.add(Component.text("  Status  ", Menus.MUTED)
                .append(Component.text(enabled ? "● Enabled" : "✖ Disabled", enabled ? Menus.SUCCESS : Menus.DANGER)));
            if (experimental) {
                lore.add(Component.text("  Type  ", Menus.MUTED).append(Component.text("Experimental", Menus.PURPLE)));
            }
            lore.add(Component.empty());
            lore.add(Component.text("  ▸ Click to " + (enabled ? "disable" : "enable"), enabled ? Menus.DANGER : Menus.SUCCESS));

            ItemStack ic = Menus.item(mat, nameComp, lore);
            Menus.setPDC(ic, Menus.KEY_TYPE, "check");
            Menus.setPDC(ic, Menus.KEY_VALUE, check.getCheckName());
            inventory.setItem(Menus.CONTENT_SLOTS[slot++], ic);
        }

        // Back button (slot 45)
        ItemStack back = Menus.item(Material.SPECTRAL_ARROW,
            Component.text("← ", Menus.MUTED).append(Component.text("Back to Panel", Menus.SOFT_WHITE)),
            null);
        Menus.setPDC(back, Menus.KEY_TYPE, "back");
        inventory.setItem(45, back);

        // Reset All VLs button (slot 53)
        ItemStack reset = Menus.item(Material.BARRIER,
            Component.text("⚠ ", Menus.DANGER).append(Component.text("Reset All VLs", Menus.DANGER)),
            List.of(
                Component.empty(),
                Component.text("  Resets violations for all checks", Menus.MUTED),
                Component.text("  ▸ Click to confirm", Menus.HIGHLIGHT)
            ));
        Menus.setPDC(reset, Menus.KEY_TYPE, "reset");
        inventory.setItem(53, reset);

        // Spartan stats (slot 49)
        if (SpartanCrossCheck.isAvailable() && target != null) {
            var stats = SpartanCrossCheck.getStats(target.uuid);
            ItemStack spartan = Menus.item(Material.ENCHANTED_BOOK,
                Component.text("◆ ", Menus.ACCENT2).append(Component.text("Spartan Cross-Check", Menus.SOFT_WHITE)),
                List.of(
                    Component.empty(),
                    Component.text("  ✔ Agreements  ", Menus.MUTED).append(Component.text(String.valueOf(stats.agreements), Menus.SUCCESS)),
                    Component.text("  ✖ Disagreements  ", Menus.MUTED).append(Component.text(String.valueOf(stats.disagreements), Menus.DANGER)),
                    Component.text("  Rate  ", Menus.MUTED).append(Component.text(String.format("%.0f%%", stats.agreementRate() * 100), Menus.INFO)),
                    Component.empty()
                ));
            inventory.setItem(49, spartan);
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
            case "check" -> {
                toggleCheck(viewer, value);
                refresh();
            }
            case "reset" -> {
                resetAllVLs(viewer);
                refresh();
            }
            case "back" -> new HubMenu().open(viewer);
        }
    }

    private void toggleCheck(Player viewer, String checkName) {
        SacPlayer target = Menus.resolveSacPlayer(targetName);
        if (target == null) return;
        for (var entry : target.checkManager.allChecks.entrySet()) {
            Check c = (Check) entry.getValue();
            if (c.getCheckName() != null && c.getCheckName().equalsIgnoreCase(checkName)) {
                boolean newState = !c.isEnabled();
                c.setEnabled(newState);
                Menus.tell(viewer, Component.text()
                    .append(Component.text("  " + checkName + " ", Menus.SOFT_WHITE))
                    .append(Component.text("→ ", Menus.MUTED))
                    .append(Component.text(newState ? "ENABLED" : "DISABLED", newState ? Menus.SUCCESS : Menus.DANGER))
                    .append(Component.text(" for " + targetName, Menus.MUTED))
                    .build());
                return;
            }
        }
    }

    private void resetAllVLs(Player viewer) {
        SacPlayer target = Menus.resolveSacPlayer(targetName);
        if (target == null) return;
        int count = 0;
        for (var entry : target.checkManager.allChecks.entrySet()) {
            ((Check) entry.getValue()).violations = 0;
            count++;
        }
        Menus.tell(viewer, Component.text()
            .append(Component.text("  ✔ Reset ", Menus.SUCCESS))
            .append(Component.text(count + " checks", Menus.SOFT_WHITE))
            .append(Component.text(" for " + targetName, Menus.MUTED))
            .build());
    }
}
