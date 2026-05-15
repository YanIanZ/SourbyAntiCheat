package dev.yanianz.sourbyanticheat.platform.bukkit.gui;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class SacGUI implements Listener {

    private static final String MAIN_TITLE = "SAC Panel";
    private static final String CHECK_TITLE = " Checks";
    private static final NamespacedKey KEY_TYPE = new NamespacedKey("sac", "gui_type");
    private static final NamespacedKey KEY_VALUE = new NamespacedKey("sac", "gui_value");

    public static void openMain(Player player) {
        var pdm = SacAPI.INSTANCE.getPlayerDataManager();
        var entries = new ArrayList<>(pdm.getEntries());
        entries.sort(Comparator.comparing(SacPlayer::getName, String.CASE_INSENSITIVE_ORDER));

        Inventory inv = Bukkit.createInventory(null, 54, Component.text(MAIN_TITLE, NamedTextColor.DARK_RED));

        for (int i = 0; i < Math.min(entries.size(), 45); i++) {
            SacPlayer sp = entries.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = head.getItemMeta();
            meta.displayName(Component.text(sp.getName(), NamedTextColor.YELLOW));
            int totalVL = (int) sp.checkManager.allChecks.values().stream()
                .mapToDouble(c -> ((Check)c).violations).sum();
            meta.lore(List.of(
                Component.text("Total VL: " + totalVL, totalVL > 50 ? NamedTextColor.RED : NamedTextColor.GREEN),
                Component.text("Click to manage", NamedTextColor.GRAY)
            ));
            head.setItemMeta(meta);
            setPDC(head, KEY_TYPE, "player");
            setPDC(head, KEY_VALUE, sp.getName());
            inv.setItem(i, head);
        }

        ItemStack status = new ItemStack(Material.NETHER_STAR);
        ItemMeta sm = status.getItemMeta();
        sm.displayName(Component.text("SAC Status", NamedTextColor.GOLD));
        sm.lore(List.of(
            Component.text("Tracked: " + entries.size(), NamedTextColor.WHITE),
            Component.text("Platform: " + SacAPI.INSTANCE.getPlatform().name(), NamedTextColor.WHITE)
        ));
        status.setItemMeta(sm);
        setPDC(status, KEY_TYPE, "status");
        inv.setItem(49, status);

        if (SpartanCrossCheck.isAvailable()) {
            ItemStack spartan = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta spm = spartan.getItemMeta();
            spm.displayName(Component.text("SpartanAPI Active", NamedTextColor.GOLD));
            spm.lore(List.of(Component.text("Cross-check enabled", NamedTextColor.GREEN)));
            spartan.setItemMeta(spm);
            inv.setItem(51, spartan);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (title == null || (!title.equals(MAIN_TITLE) && !title.endsWith(CHECK_TITLE))) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        String type = getPDC(item, KEY_TYPE);
        String value = getPDC(item, KEY_VALUE);
        if (type == null) return;

        switch (type) {
            case "player" -> openPlayerDetail(player, value);
            case "check" -> {
                String playerName = title.replace(CHECK_TITLE, "");
                toggleCheck(player, playerName, value);
                openPlayerDetail(player, playerName);
            }
            case "status" -> { player.closeInventory(); player.performCommand("sac status"); }
            case "back" -> openMain(player);
            case "reset" -> {
                String playerName = title.replace(CHECK_TITLE, "");
                resetAllVLs(player, playerName);
                openPlayerDetail(player, playerName);
            }
        }
    }

    private static String getPDC(ItemStack item, NamespacedKey key) {
        var meta = item.getItemMeta();
        return meta != null ? meta.getPersistentDataContainer().get(key, PersistentDataType.STRING) : null;
    }

    private static void setPDC(ItemStack item, NamespacedKey key, String value) {
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
            item.setItemMeta(meta);
        }
    }

    private static void openPlayerDetail(Player viewer, String targetName) {
        var pdm = SacAPI.INSTANCE.getPlayerDataManager();
        SacPlayer target = null;
        for (SacPlayer sp : pdm.getEntries()) {
            if (sp.getName().equalsIgnoreCase(targetName)) { target = sp; break; }
        }
        if (target == null) {
            viewer.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54,
            Component.text(targetName + " Checks", NamedTextColor.DARK_RED));

        var checks = new ArrayList<>(target.checkManager.allChecks.entrySet());
        checks.sort(Comparator.comparing(e -> ((Check)e.getValue()).getCheckName() == null
            ? "" : ((Check)e.getValue()).getCheckName(), String.CASE_INSENSITIVE_ORDER));

        int slot = 0;
        for (var entry : checks) {
            Check check = (Check) entry.getValue();
            if (check.getCheckName() == null) continue;
            if (slot >= 45) break;

            ItemStack ic = new ItemStack(check.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE);
            ItemMeta im = ic.getItemMeta();
            im.displayName(Component.text(check.getCheckName(), check.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
            im.lore(List.of(
                Component.text("VL: " + String.format("%.1f", check.violations), NamedTextColor.YELLOW),
                Component.text("Enabled: " + check.isEnabled(), check.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED),
                Component.text("Click to toggle on/off", NamedTextColor.GRAY)
            ));
            ic.setItemMeta(im);
            setPDC(ic, KEY_TYPE, "check");
            setPDC(ic, KEY_VALUE, check.getCheckName());
            inv.setItem(slot++, ic);
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.displayName(Component.text("Back to Main", NamedTextColor.GRAY));
        back.setItemMeta(bm);
        setPDC(back, KEY_TYPE, "back");

        ItemStack reset = new ItemStack(Material.TNT);
        ItemMeta rm = reset.getItemMeta();
        rm.displayName(Component.text("Reset All VLs", NamedTextColor.RED));
        reset.setItemMeta(rm);
        setPDC(reset, KEY_TYPE, "reset");
        inv.setItem(53, reset);

        if (SpartanCrossCheck.isAvailable()) {
            var stats = SpartanCrossCheck.getStats(target.uuid);
            ItemStack spartan = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta spm = spartan.getItemMeta();
            spm.displayName(Component.text("SpartanAPI Cross-Check", NamedTextColor.GOLD));
            spm.lore(List.of(
                Component.text("Agreements: " + stats.agreements, NamedTextColor.GREEN),
                Component.text("Disagreements: " + stats.disagreements, NamedTextColor.RED),
                Component.text("Rate: " + String.format("%.0f%%", stats.agreementRate() * 100), NamedTextColor.AQUA)
            ));
            spartan.setItemMeta(spm);
            inv.setItem(51, spartan);
        }

        viewer.openInventory(inv);
    }

    private static void toggleCheck(Player viewer, String targetName, String checkName) {
        var pdm = SacAPI.INSTANCE.getPlayerDataManager();
        SacPlayer target = null;
        for (SacPlayer sp : pdm.getEntries()) {
            if (sp.getName().equalsIgnoreCase(targetName)) { target = sp; break; }
        }
        if (target == null) return;

        for (var entry : target.checkManager.allChecks.entrySet()) {
            Check c = (Check) entry.getValue();
            if (c.getCheckName() != null && c.getCheckName().equalsIgnoreCase(checkName)) {
                boolean newState = !c.isEnabled();
                c.setEnabled(newState);
                viewer.sendMessage(Component.text(checkName + " → " + (newState ? "ENABLED" : "DISABLED") + " for " + targetName,
                    newState ? NamedTextColor.GREEN : NamedTextColor.RED));
                return;
            }
        }
    }

    private static void resetAllVLs(Player viewer, String targetName) {
        var pdm = SacAPI.INSTANCE.getPlayerDataManager();
        SacPlayer target = null;
        for (SacPlayer sp : pdm.getEntries()) {
            if (sp.getName().equalsIgnoreCase(targetName)) { target = sp; break; }
        }
        if (target == null) return;

        int count = 0;
        for (var entry : target.checkManager.allChecks.entrySet()) {
            ((Check) entry.getValue()).violations = 0;
            count++;
        }
        viewer.sendMessage(Component.text("Reset " + count + " checks for " + targetName, NamedTextColor.GREEN));
    }
}
