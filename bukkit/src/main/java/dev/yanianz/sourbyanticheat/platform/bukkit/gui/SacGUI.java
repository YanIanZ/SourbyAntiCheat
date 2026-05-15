package dev.yanianz.sourbyanticheat.platform.bukkit.gui;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Premium SAC Control Panel GUI with modern styling.
 */
public class SacGUI implements Listener {

    // ── Color palette (matches SacColors hex) ────────────────────────
    private static final TextColor BRAND      = TextColor.fromHexString("#FF6B35");
    private static final TextColor ACCENT     = TextColor.fromHexString("#00D4AA");
    private static final TextColor ACCENT2    = TextColor.fromHexString("#7C4DFF");
    private static final TextColor HIGHLIGHT  = TextColor.fromHexString("#FFD740");
    private static final TextColor SUCCESS    = TextColor.fromHexString("#4ADE80");
    private static final TextColor DANGER     = TextColor.fromHexString("#F87171");
    private static final TextColor INFO       = TextColor.fromHexString("#22D3EE");
    private static final TextColor SOFT_WHITE = TextColor.fromHexString("#F1F5F9");
    private static final TextColor MUTED      = TextColor.fromHexString("#94A3B8");
    private static final TextColor DARK_MUTED = TextColor.fromHexString("#64748B");
    private static final TextColor PURPLE     = TextColor.fromHexString("#C084FC");

    private static final String MAIN_TITLE = "SAC Panel";
    private static final String CHECK_TITLE = " Checks";
    private static final NamespacedKey KEY_TYPE = new NamespacedKey("sac", "gui_type");
    private static final NamespacedKey KEY_VALUE = new NamespacedKey("sac", "gui_value");

    // ── Main Panel ───────────────────────────────────────────────────
    public static void openMain(Player player) {
        var pdm = SacAPI.INSTANCE.getPlayerDataManager();
        var entries = new ArrayList<>(pdm.getEntries());
        entries.sort(Comparator.comparing(SacPlayer::getName, String.CASE_INSENSITIVE_ORDER));

        Inventory inv = Bukkit.createInventory(null, 54,
            Component.text("✦ ", BRAND).append(Component.text("SAC Control Panel", SOFT_WHITE, TextDecoration.BOLD)));

        // ── Glass border (top + bottom row) ──
        fillBorder(inv);

        // ── Player heads (slots 10-43, skipping borders) ──
        int[] contentSlots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43};
        for (int i = 0; i < Math.min(entries.size(), contentSlots.length); i++) {
            SacPlayer sp = entries.get(i);
            int totalVL = getTotalVL(sp);
            TextColor vlColor = vlColor(totalVL);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(sp.uuid));
            meta.displayName(Component.text("✦ ", ACCENT).append(Component.text(sp.getName(), SOFT_WHITE, TextDecoration.BOLD)));

            long activeChecks = sp.checkManager.allChecks.values().stream()
                .filter(c -> ((Check) c).violations > 0.5).count();

            meta.lore(List.of(
                Component.empty(),
                Component.text("  VL  ", MUTED).append(Component.text(totalVL, vlColor, TextDecoration.BOLD)),
                Component.text("  Active Flags  ", MUTED).append(Component.text(activeChecks, activeChecks > 0 ? HIGHLIGHT : DARK_MUTED)),
                Component.text("  Ping  ", MUTED).append(Component.text(sp.getTransactionPing() + "ms", DARK_MUTED)),
                Component.empty(),
                Component.text("  ▸ Click to inspect", ACCENT)
            ));
            head.setItemMeta(meta);
            setPDC(head, KEY_TYPE, "player");
            setPDC(head, KEY_VALUE, sp.getName());
            inv.setItem(contentSlots[i], head);
        }

        // ── Status beacon (bottom center) ──
        ItemStack status = new ItemStack(Material.NETHER_STAR);
        ItemMeta sm = status.getItemMeta();
        sm.displayName(Component.text("★ ", HIGHLIGHT).append(Component.text("System Status", SOFT_WHITE, TextDecoration.BOLD)));

        boolean nettyOk = !dev.yanianz.sourbyanticheat.netty.SacNettyInjector.isInjectionFailed();
        boolean spartanOk = SpartanCrossCheck.isAvailable();

        sm.lore(List.of(
            Component.empty(),
            Component.text("  Tracked  ", MUTED).append(Component.text(entries.size() + " players", SOFT_WHITE)),
            Component.text("  Platform  ", MUTED).append(Component.text(SacAPI.INSTANCE.getPlatform().name(), ACCENT)),
            Component.text("  Netty  ", MUTED).append(Component.text(nettyOk ? "● Active" : "✖ Failed", nettyOk ? SUCCESS : DANGER)),
            Component.text("  Spartan  ", MUTED).append(Component.text(spartanOk ? "● Active" : "○ Off", spartanOk ? SUCCESS : DARK_MUTED)),
            Component.empty(),
            Component.text("  ▸ Click for details", ACCENT)
        ));
        status.setItemMeta(sm);
        setPDC(status, KEY_TYPE, "status");
        inv.setItem(49, status);

        // ── Spartan icon ──
        if (spartanOk) {
            ItemStack spartan = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta spm = spartan.getItemMeta();
            spm.displayName(Component.text("◆ ", ACCENT2).append(Component.text("SpartanAPI", SOFT_WHITE)));
            long agrees = SpartanCrossCheck.getAgreements();
            long total = SpartanCrossCheck.getTotalFlags();
            spm.lore(List.of(
                Component.empty(),
                Component.text("  Cross-check  ", MUTED).append(Component.text("ENABLED", SUCCESS)),
                Component.text("  Agreements  ", MUTED).append(Component.text(agrees + "/" + total, INFO)),
                Component.empty()
            ));
            spartan.setItemMeta(spm);
            inv.setItem(51, spartan);
        }

        player.openInventory(inv);
    }

    // ── Player Detail Panel ──────────────────────────────────────────
    private static void openPlayerDetail(Player viewer, String targetName) {
        var pdm = SacAPI.INSTANCE.getPlayerDataManager();
        SacPlayer target = null;
        for (SacPlayer sp : pdm.getEntries()) {
            if (sp.getName().equalsIgnoreCase(targetName)) { target = sp; break; }
        }
        if (target == null) {
            viewer.sendMessage(Component.text("Player not found.", DANGER));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54,
            Component.text("✦ ", ACCENT).append(Component.text(targetName, SOFT_WHITE, TextDecoration.BOLD))
                .append(Component.text(" — Checks", MUTED)));

        fillBorder(inv);

        var checks = new ArrayList<>(target.checkManager.allChecks.entrySet());
        checks.sort(Comparator.comparing(e -> ((Check) e.getValue()).getCheckName() == null
            ? "" : ((Check) e.getValue()).getCheckName(), String.CASE_INSENSITIVE_ORDER));

        int[] contentSlots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43};
        int slot = 0;
        for (var entry : checks) {
            Check check = (Check) entry.getValue();
            if (check.getCheckName() == null) continue;
            if (slot >= contentSlots.length) break;

            double vl = check.violations;
            boolean enabled = check.isEnabled();
            boolean experimental = check.isExperimental();

            Material mat;
            if (!enabled) mat = Material.GRAY_STAINED_GLASS_PANE;
            else if (vl > 50) mat = Material.RED_CONCRETE;
            else if (vl > 10) mat = Material.ORANGE_CONCRETE;
            else if (vl > 1) mat = Material.YELLOW_CONCRETE;
            else mat = Material.LIME_CONCRETE;

            ItemStack ic = new ItemStack(mat);
            ItemMeta im = ic.getItemMeta();

            Component nameComp = Component.text(check.getCheckName(), enabled ? SOFT_WHITE : DARK_MUTED);
            if (experimental) nameComp = nameComp.append(Component.text(" *", PURPLE));
            im.displayName(nameComp);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("  VL  ", MUTED)
                .append(Component.text(String.format("%.1f", vl), vlColor(vl), TextDecoration.BOLD)));
            lore.add(Component.text("  Status  ", MUTED)
                .append(Component.text(enabled ? "● Enabled" : "✖ Disabled", enabled ? SUCCESS : DANGER)));
            if (experimental) {
                lore.add(Component.text("  Type  ", MUTED).append(Component.text("Experimental", PURPLE)));
            }
            lore.add(Component.empty());
            lore.add(Component.text("  ▸ Click to " + (enabled ? "disable" : "enable"), enabled ? DANGER : SUCCESS));

            im.lore(lore);
            ic.setItemMeta(im);
            setPDC(ic, KEY_TYPE, "check");
            setPDC(ic, KEY_VALUE, check.getCheckName());
            inv.setItem(contentSlots[slot++], ic);
        }

        // ── Back button ──
        ItemStack back = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.displayName(Component.text("← ", MUTED).append(Component.text("Back to Panel", SOFT_WHITE)));
        back.setItemMeta(bm);
        setPDC(back, KEY_TYPE, "back");
        inv.setItem(45, back);

        // ── Reset button ──
        ItemStack reset = new ItemStack(Material.BARRIER);
        ItemMeta rm = reset.getItemMeta();
        rm.displayName(Component.text("⚠ ", DANGER).append(Component.text("Reset All VLs", DANGER)));
        rm.lore(List.of(
            Component.empty(),
            Component.text("  Resets violations for all checks", MUTED),
            Component.text("  ▸ Click to confirm", HIGHLIGHT)
        ));
        reset.setItemMeta(rm);
        setPDC(reset, KEY_TYPE, "reset");
        inv.setItem(53, reset);

        // ── Spartan Cross-Check ──
        if (SpartanCrossCheck.isAvailable()) {
            var stats = SpartanCrossCheck.getStats(target.uuid);
            ItemStack spartan = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta spm = spartan.getItemMeta();
            spm.displayName(Component.text("◆ ", ACCENT2).append(Component.text("Spartan Cross-Check", SOFT_WHITE)));
            spm.lore(List.of(
                Component.empty(),
                Component.text("  ✔ Agreements  ", MUTED).append(Component.text(String.valueOf(stats.agreements), SUCCESS)),
                Component.text("  ✖ Disagreements  ", MUTED).append(Component.text(String.valueOf(stats.disagreements), DANGER)),
                Component.text("  Rate  ", MUTED).append(Component.text(String.format("%.0f%%", stats.agreementRate() * 100), INFO)),
                Component.empty()
            ));
            spartan.setItemMeta(spm);
            inv.setItem(49, spartan);
        }

        viewer.openInventory(inv);
    }

    // ── Event Handler ────────────────────────────────────────────────
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (title == null) return;
        // Match our GUI titles by checking for our Unicode markers
        boolean isMainPanel = title.contains("SAC Control Panel");
        boolean isCheckPanel = title.contains("— Checks");
        if (!isMainPanel && !isCheckPanel) return;
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
                // Extract player name from title: "✦ PlayerName — Checks"
                String playerName = extractPlayerName(title);
                if (playerName != null) {
                    toggleCheck(player, playerName, value);
                    openPlayerDetail(player, playerName);
                }
            }
            case "status" -> { player.closeInventory(); player.performCommand("sac status"); }
            case "back" -> openMain(player);
            case "reset" -> {
                String playerName = extractPlayerName(title);
                if (playerName != null) {
                    resetAllVLs(player, playerName);
                    openPlayerDetail(player, playerName);
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private static String extractPlayerName(String title) {
        // Title format: "✦ PlayerName — Checks"
        int dash = title.indexOf("—");
        if (dash < 0) return null;
        String name = title.substring(0, dash).trim();
        // Remove leading "✦ "
        if (name.startsWith("✦")) name = name.substring(1).trim();
        return name.isEmpty() ? null : name;
    }

    private static void fillBorder(Inventory inv) {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta pm = pane.getItemMeta();
        pm.displayName(Component.empty());
        pane.setItemMeta(pm);

        // Top row
        for (int i = 0; i < 9; i++) inv.setItem(i, pane.clone());
        // Bottom row
        for (int i = 45; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, pane.clone());
        }
        // Side borders
        for (int row = 1; row < 5; row++) {
            inv.setItem(row * 9, pane.clone());
            inv.setItem(row * 9 + 8, pane.clone());
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

    private static int getTotalVL(SacPlayer sp) {
        return (int) sp.checkManager.allChecks.values().stream()
            .mapToDouble(c -> ((Check) c).violations).sum();
    }

    private static TextColor vlColor(double vl) {
        if (vl >= 100) return DANGER;
        if (vl >= 50) return TextColor.fromHexString("#FBBF24");
        if (vl >= 10) return HIGHLIGHT;
        return SUCCESS;
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
                viewer.sendMessage(Component.text()
                    .append(Component.text("  " + checkName + " ", SOFT_WHITE))
                    .append(Component.text("→ ", MUTED))
                    .append(Component.text(newState ? "ENABLED" : "DISABLED", newState ? SUCCESS : DANGER))
                    .append(Component.text(" for " + targetName, MUTED))
                    .build());
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
        viewer.sendMessage(Component.text()
            .append(Component.text("  ✔ Reset ", SUCCESS))
            .append(Component.text(count + " checks", SOFT_WHITE))
            .append(Component.text(" for " + targetName, MUTED))
            .build());
    }
}
