package dev.yanianz.sourbyanticheat.platform.bukkit.gui.menu;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.manager.ReportManager;
import dev.yanianz.sourbyanticheat.manager.WavePunishment;
import dev.yanianz.sourbyanticheat.netty.SacNettyInjector;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/** Main hub menu — entry point for the SAC control panel. */
public final class HubMenu extends SacMenu {

    @Override
    protected int size() { return 54; }

    @Override
    protected Component title() {
        return Component.text("✦ ", Menus.BRAND)
            .append(Component.text("SAC Control Panel", Menus.SOFT_WHITE, TextDecoration.BOLD));
    }

    @Override
    protected void render() {
        Menus.fillBorder(inventory);

        var pdm = SacAPI.INSTANCE.getPlayerDataManager();
        var entries = new java.util.ArrayList<>(pdm.getEntries());
        int trackedCount = entries.size();
        boolean nettyOk  = !SacNettyInjector.isInjectionFailed();
        boolean spartanOk = SpartanCrossCheck.isAvailable();

        // Players button (slot 19)
        ItemStack players = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta pm = players.getItemMeta();
        pm.displayName(Component.text("👤 ", Menus.ACCENT).append(Component.text("Players", Menus.SOFT_WHITE)));
        pm.lore(List.of(
            Component.empty(),
            Component.text("  Tracked  ", Menus.MUTED).append(Component.text(trackedCount + " players", Menus.SOFT_WHITE)),
            Component.empty(),
            Component.text("  ▸ Click to inspect", Menus.ACCENT)
        ));
        players.setItemMeta(pm);
        Menus.setPDC(players, Menus.KEY_TYPE, "nav_players");
        inventory.setItem(19, players);

        // Profiles button (slot 21)
        ItemStack profiles = new ItemStack(Material.COMPASS);
        ItemMeta pfm = profiles.getItemMeta();
        pfm.displayName(Component.text("⚙ ", Menus.ACCENT2).append(Component.text("Profiles", Menus.SOFT_WHITE)));
        pfm.lore(List.of(
            Component.empty(),
            Component.text("  Per-arena anticheat profiles", Menus.MUTED),
            Component.text("  Edit disabled checks + overrides", Menus.MUTED),
            Component.empty(),
            Component.text("  ▸ Click to manage", Menus.ACCENT)
        ));
        profiles.setItemMeta(pfm);
        Menus.setPDC(profiles, Menus.KEY_TYPE, "nav_profiles");
        inventory.setItem(21, profiles);

        // Alerts button (slot 23)
        ItemStack alerts = new ItemStack(Material.BELL);
        ItemMeta am = alerts.getItemMeta();
        am.displayName(Component.text("🔔 ", Menus.HIGHLIGHT).append(Component.text("Alerts", Menus.SOFT_WHITE)));
        am.lore(List.of(
            Component.empty(),
            Component.text("  Recent staff alert feed", Menus.MUTED),
            Component.empty(),
            Component.text("  ▸ Click to view", Menus.ACCENT)
        ));
        alerts.setItemMeta(am);
        Menus.setPDC(alerts, Menus.KEY_TYPE, "nav_alerts");
        inventory.setItem(23, alerts);

        // Reports button (slot 25)
        int reportCount = ReportManager.getAllReports().size();
        ItemStack reports = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta rm = reports.getItemMeta();
        rm.displayName(Component.text("📋 ", Menus.ACCENT).append(Component.text("Reports", Menus.SOFT_WHITE)));
        rm.lore(List.of(
            Component.empty(),
            Component.text("  Pending  ", Menus.MUTED).append(Component.text(String.valueOf(reportCount), reportCount > 0 ? Menus.HIGHLIGHT : Menus.DARK_MUTED)),
            Component.empty(),
            Component.text("  ▸ Click to manage", Menus.ACCENT)
        ));
        reports.setItemMeta(rm);
        Menus.setPDC(reports, Menus.KEY_TYPE, "nav_reports");
        inventory.setItem(25, reports);

        // Wave Queue button (slot 31)
        int waveCount = WavePunishment.queueView().size();
        ItemStack wave = new ItemStack(Material.CLOCK);
        ItemMeta wm = wave.getItemMeta();
        wm.displayName(Component.text("⏳ ", Menus.ACCENT2).append(Component.text("Wave Queue", Menus.SOFT_WHITE)));
        wm.lore(List.of(
            Component.empty(),
            Component.text("  Queued  ", Menus.MUTED).append(Component.text(waveCount + " entries", waveCount > 0 ? Menus.HIGHLIGHT : Menus.DARK_MUTED)),
            Component.empty(),
            Component.text("  ▸ Click to view", Menus.ACCENT)
        ));
        wave.setItemMeta(wm);
        Menus.setPDC(wave, Menus.KEY_TYPE, "nav_wave");
        inventory.setItem(31, wave);

        // System Status beacon (slot 49)
        ItemStack status = new ItemStack(Material.NETHER_STAR);
        ItemMeta sm = status.getItemMeta();
        sm.displayName(Component.text("★ ", Menus.HIGHLIGHT)
            .append(Component.text("System Status", Menus.SOFT_WHITE, TextDecoration.BOLD)));
        sm.lore(List.of(
            Component.empty(),
            Component.text("  Tracked  ", Menus.MUTED).append(Component.text(trackedCount + " players", Menus.SOFT_WHITE)),
            Component.text("  Platform  ", Menus.MUTED).append(Component.text(SacAPI.INSTANCE.getPlatform().name(), Menus.ACCENT)),
            Component.text("  Netty  ", Menus.MUTED).append(Component.text(nettyOk ? "● Active" : "✖ Failed", nettyOk ? Menus.SUCCESS : Menus.DANGER)),
            Component.text("  Spartan  ", Menus.MUTED).append(Component.text(spartanOk ? "● Active" : "○ Off", spartanOk ? Menus.SUCCESS : Menus.DARK_MUTED)),
            Component.empty(),
            Component.text("  ▸ Click for details", Menus.ACCENT)
        ));
        status.setItemMeta(sm);
        Menus.setPDC(status, Menus.KEY_TYPE, "status");
        inventory.setItem(49, status);

        // Spartan icon (slot 51) — only if Spartan is active
        if (spartanOk) {
            ItemStack spartan = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta spm = spartan.getItemMeta();
            spm.displayName(Component.text("◆ ", Menus.ACCENT2).append(Component.text("SpartanAPI", Menus.SOFT_WHITE)));
            long agrees = SpartanCrossCheck.getAgreements();
            long total  = SpartanCrossCheck.getTotalFlags();
            spm.lore(List.of(
                Component.empty(),
                Component.text("  Cross-check  ", Menus.MUTED).append(Component.text("ENABLED", Menus.SUCCESS)),
                Component.text("  Agreements  ", Menus.MUTED).append(Component.text(agrees + "/" + total, Menus.INFO)),
                Component.empty()
            ));
            spartan.setItemMeta(spm);
            inventory.setItem(51, spartan);
        }
    }

    @Override
    public void onClick(Player viewer, int slot, InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        String type = Menus.getPDC(item, Menus.KEY_TYPE);
        if (type == null) return;

        switch (type) {
            case "nav_players"  -> new PlayersMenu(0).open(viewer);
            case "nav_profiles" -> new ProfilesMenu().open(viewer);
            case "nav_alerts"   -> new AlertsMenu().open(viewer);
            case "nav_reports"  -> new ReportsMenu().open(viewer);
            case "nav_wave"     -> new WaveMenu().open(viewer);
            case "status"       -> { viewer.closeInventory(); viewer.performCommand("sac status"); }
        }
    }
}
