package dev.yanianz.sourbyanticheat.platform.bukkit.gui.menu;

import dev.yanianz.sourbyanticheat.profile.Profile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Lists all six profiles; click to open the detail/edit panel. */
public final class ProfilesMenu extends SacMenu {

    // One material per profile for visual distinction
    private static final Material[] PROFILE_MATERIALS = {
        Material.RED_BED,       // BEDWARS
        Material.BLUE_BED,      // SKYWARS
        Material.CYAN_BED,      // SKYBLOCK
        Material.LIME_BED,      // PRACTICE
        Material.YELLOW_BED,    // LOBBY
        Material.WHITE_BED      // GENERIC
    };

    @Override protected int size() { return 54; }

    @Override
    protected Component title() {
        return Component.text("⚙ ", Menus.ACCENT2)
            .append(Component.text("Profiles", Menus.SOFT_WHITE, TextDecoration.BOLD));
    }

    @Override
    protected void render() {
        Menus.fillBorder(inventory);

        Profile[] profiles = Profile.values();
        // Spread them across the middle rows
        int[] slots = {19, 21, 23, 28, 30, 32};
        for (int i = 0; i < profiles.length; i++) {
            Profile p = profiles[i];
            Material mat = i < PROFILE_MATERIALS.length ? PROFILE_MATERIALS[i] : Material.CYAN_STAINED_GLASS_PANE;
            ItemStack item = Menus.item(mat,
                Component.text("▸ ", Menus.ACCENT).append(Component.text(p.name(), Menus.SOFT_WHITE, TextDecoration.BOLD)),
                List.of(
                    Component.empty(),
                    Component.text("  Edit disabled checks,", Menus.MUTED),
                    Component.text("  overrides and leniencies", Menus.MUTED),
                    Component.empty(),
                    Component.text("  ▸ Click to configure", Menus.ACCENT)
                ));
            Menus.setPDC(item, Menus.KEY_TYPE, "profile");
            Menus.setPDC(item, Menus.KEY_VALUE, p.name());
            inventory.setItem(slots[i], item);
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
            case "profile" -> {
                Profile p = Profile.fromString(value);
                new ProfileDetailMenu(p).open(viewer);
            }
            case "back" -> new HubMenu().open(viewer);
        }
    }
}
