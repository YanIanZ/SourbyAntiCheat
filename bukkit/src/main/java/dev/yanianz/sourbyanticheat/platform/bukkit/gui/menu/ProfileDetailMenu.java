package dev.yanianz.sourbyanticheat.platform.bukkit.gui.menu;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.profile.Profile;
import dev.yanianz.sourbyanticheat.profile.ProfileConfigSnapshot;
import dev.yanianz.sourbyanticheat.profile.ProfileConfigWriter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shows a profile's disabled checks (removable), overrides (adjustable with
 * left/right click), and leniencies (view-only).
 */
public final class ProfileDetailMenu extends SacMenu {

    private final Profile profile;

    public ProfileDetailMenu(Profile profile) {
        this.profile = profile;
    }

    @Override protected int size() { return 54; }

    @Override
    protected Component title() {
        return Component.text("⚙ ", Menus.ACCENT2)
            .append(Component.text("Profile: ", Menus.MUTED))
            .append(Component.text(profile.name(), Menus.SOFT_WHITE, TextDecoration.BOLD));
    }

    @Override
    protected void render() {
        Menus.fillBorder(inventory);

        var pc = SacAPI.INSTANCE.getProfileConfig();
        ProfileConfigSnapshot.ProfileSection section = pc.snapshot().forProfile(profile);

        int slot = 0;

        // ── Section label: Disabled checks ───────────────────────────
        if (!section.disabled.isEmpty()) {
            for (String checkName : section.disabled) {
                if (slot >= Menus.CONTENT_SLOTS.length) break;
                ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                ItemMeta im = item.getItemMeta();
                im.displayName(Component.text("✖ ", Menus.DANGER).append(Component.text(checkName, Menus.SOFT_WHITE)));
                im.lore(List.of(
                    Component.empty(),
                    Component.text("  Status  ", Menus.MUTED).append(Component.text("DISABLED in this profile", Menus.DANGER)),
                    Component.empty(),
                    Component.text("  ▸ Click to re-enable (remove from disabled)", Menus.SUCCESS)
                ));
                item.setItemMeta(im);
                Menus.setPDC(item, Menus.KEY_TYPE, "prof_disabled");
                Menus.setPDC(item, Menus.KEY_VALUE, checkName);
                inventory.setItem(Menus.CONTENT_SLOTS[slot++], item);
            }
        } else {
            // Empty state placeholder for disabled section
            if (slot < Menus.CONTENT_SLOTS.length) {
                ItemStack empty = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                ItemMeta em = empty.getItemMeta();
                em.displayName(Component.text("✔ No disabled checks", Menus.SUCCESS));
                empty.setItemMeta(em);
                inventory.setItem(Menus.CONTENT_SLOTS[slot++], empty);
            }
        }

        // ── Section: Overrides ────────────────────────────────────────
        for (Map.Entry<String, Map<String, Object>> overrideEntry : section.overrides.entrySet()) {
            if (slot >= Menus.CONTENT_SLOTS.length) break;
            String checkName = overrideEntry.getKey();
            Map<String, Object> kvMap = overrideEntry.getValue();

            // Find the first numeric key for adjust support
            String firstNumericKey = null;
            Object firstNumericValue = null;
            for (Map.Entry<String, Object> kv : kvMap.entrySet()) {
                if (kv.getValue() instanceof Number) {
                    firstNumericKey = kv.getKey();
                    firstNumericValue = kv.getValue();
                    break;
                }
            }

            ItemStack item = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
            ItemMeta im = item.getItemMeta();
            im.displayName(Component.text("⚙ ", Menus.ACCENT).append(Component.text(checkName, Menus.SOFT_WHITE)));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            for (Map.Entry<String, Object> kv : kvMap.entrySet()) {
                lore.add(Component.text("  " + kv.getKey() + "  ", Menus.MUTED)
                    .append(Component.text(String.valueOf(kv.getValue()), Menus.HIGHLIGHT)));
            }
            lore.add(Component.empty());
            if (firstNumericKey != null) {
                lore.add(Component.text("  L-Click: +1 / R-Click: -1", Menus.ACCENT));
                lore.add(Component.text("  Shift: ×5 step", Menus.MUTED));
            }
            im.lore(lore);
            item.setItemMeta(im);
            Menus.setPDC(item, Menus.KEY_TYPE, "prof_override");
            Menus.setPDC(item, Menus.KEY_VALUE, checkName);
            if (firstNumericKey != null) {
                Menus.setPDC(item, Menus.KEY_UUID, firstNumericKey); // reuse KEY_UUID to carry the key name
            }
            inventory.setItem(Menus.CONTENT_SLOTS[slot++], item);
        }

        // ── Section: Leniencies (view-only) ──────────────────────────
        for (ProfileConfigSnapshot.LeniencyEntry leniency : section.leniencies) {
            if (slot >= Menus.CONTENT_SLOTS.length) break;
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta im = item.getItemMeta();
            im.displayName(Component.text("☀ ", Menus.HIGHLIGHT).append(Component.text("Leniency: " + leniency.event(), Menus.SOFT_WHITE)));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("  Event  ", Menus.MUTED).append(Component.text(leniency.event(), Menus.INFO)));
            lore.add(Component.text("  Duration  ", Menus.MUTED).append(Component.text(leniency.durationMs() + "ms", Menus.SOFT_WHITE)));
            lore.add(Component.text("  Scale by amp  ", Menus.MUTED).append(Component.text(String.valueOf(leniency.scaleByAmp()), Menus.SOFT_WHITE)));
            if (!leniency.checks().isEmpty()) {
                lore.add(Component.text("  Checks  ", Menus.MUTED).append(Component.text(String.join(", ", leniency.checks()), Menus.DARK_MUTED)));
            }
            lore.add(Component.empty());
            lore.add(Component.text("  View-only — edit profiles.yml directly", Menus.DARK_MUTED));
            im.lore(lore);
            item.setItemMeta(im);
            // no PDC KEY_TYPE — view only, click does nothing actionable
            inventory.setItem(Menus.CONTENT_SLOTS[slot++], item);
        }

        // Back button (slot 45)
        ItemStack back = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.displayName(Component.text("← ", Menus.MUTED).append(Component.text("Back to Profiles", Menus.SOFT_WHITE)));
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
            case "prof_disabled" -> {
                // Remove from disabled (toggle off)
                applyEdit(viewer, () -> {
                    var pc = SacAPI.INSTANCE.getProfileConfig();
                    new ProfileConfigWriter(pc.path()).toggleDisabled(profile, value, false);
                    pc.reload();
                    SacAPI.INSTANCE.getExternalAPI().reloadAsync();
                });
            }
            case "prof_override" -> {
                String overrideKey = Menus.getPDC(item, Menus.KEY_UUID);
                if (overrideKey == null) return;
                // Get current value from snapshot
                var section = SacAPI.INSTANCE.getProfileConfig().snapshot().forProfile(profile);
                Object current = section.override(value, overrideKey);
                if (!(current instanceof Number)) return;

                boolean isInt = current instanceof Integer || current instanceof Long;
                double step;
                if (isInt) {
                    step = event.isShiftClick() ? 5.0 : 1.0;
                } else {
                    step = event.isShiftClick() ? 0.25 : 0.05;
                }
                if (event.isRightClick()) step = -step;

                final Object newValue;
                if (isInt) {
                    long v = ((Number) current).longValue() + (long) step;
                    newValue = (int) v;
                } else {
                    double v = ((Number) current).doubleValue() + step;
                    // Round to avoid floating point noise
                    v = Math.round(v * 10000.0) / 10000.0;
                    newValue = v;
                }

                final String finalKey = overrideKey;
                final String finalCheck = value;
                applyEdit(viewer, () -> {
                    var pc = SacAPI.INSTANCE.getProfileConfig();
                    new ProfileConfigWriter(pc.path()).setOverride(profile, finalCheck, finalKey, newValue);
                    pc.reload();
                    SacAPI.INSTANCE.getExternalAPI().reloadAsync();
                });
            }
            case "back" -> new ProfilesMenu().open(viewer);
        }
    }

    @FunctionalInterface
    private interface EditAction {
        void run() throws Exception;
    }

    private void applyEdit(Player viewer, EditAction action) {
        try {
            action.run();
        } catch (Exception ex) {
            viewer.sendMessage(Component.text("Edit failed: " + ex.getMessage(), Menus.DANGER));
        }
        refresh();
    }
}
