package dev.yanianz.sourbyanticheat.platform.bukkit.gui.menu;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import io.github.retrooper.packetevents.adventure.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared constants and helpers for all SAC menu classes.
 *
 * <p><b>Relocation note:</b> SAC shades and relocates Adventure
 * ({@code net.kyori} → {@code dev.yanianz.sourbyanticheat.shaded.kyori}).
 * Passing a relocated {@code Component} to Paper's {@code ItemMeta.displayName} /
 * {@code Bukkit.createInventory(..,Component)} / {@code Player.sendMessage(Component)}
 * throws {@code NoSuchMethodError} (Paper expects its own, non-relocated Component).
 * So all menu text goes through {@link #legacy(Component)} and the legacy-String
 * Bukkit API ({@code setDisplayName}/{@code setLore}/{@code createInventory(..,String)}).
 */
public final class Menus {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    /** Serialize a (relocated) Component to a legacy §-coded String — safe to
     *  hand to the native Bukkit/Paper String API across the relocation boundary. */
    public static String legacy(Component c) {
        return c == null ? "" : LEGACY.serialize(c);
    }

    /** Build an item with a legacy-String name + lore (relocation-safe). */
    public static ItemStack item(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        apply(item, name, lore);
        return item;
    }

    /** Apply a legacy-String name + lore to an existing item. */
    public static void apply(ItemStack item, Component name, List<Component> lore) {
        ItemMeta m = item.getItemMeta();
        if (m == null) return;
        if (name != null) m.setDisplayName(legacy(name));
        if (lore != null) {
            List<String> ls = new ArrayList<>(lore.size());
            for (Component c : lore) ls.add(legacy(c));
            m.setLore(ls);
        }
        item.setItemMeta(m);
    }

    /** Build a player head with owner + legacy-String name + lore. */
    public static ItemStack head(UUID owner, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta skull) {
            if (owner != null) skull.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
            skull.setDisplayName(legacy(name));
            if (lore != null) {
                List<String> ls = new ArrayList<>(lore.size());
                for (Component c : lore) ls.add(legacy(c));
                skull.setLore(ls);
            }
            item.setItemMeta(skull);
        }
        return item;
    }

    /** Send a chat message to a native Bukkit player across the relocation
     *  boundary (Player.sendMessage(Component) would throw NoSuchMethodError). */
    public static void tell(Player player, Component message) {
        player.sendMessage(legacy(message));
    }

    // ── Colour palette (ported verbatim from SacGUI) ──────────────────
    public static final TextColor BRAND      = TextColor.fromHexString("#FF6B35");
    public static final TextColor ACCENT     = TextColor.fromHexString("#00D4AA");
    public static final TextColor ACCENT2    = TextColor.fromHexString("#7C4DFF");
    public static final TextColor HIGHLIGHT  = TextColor.fromHexString("#FFD740");
    public static final TextColor SUCCESS    = TextColor.fromHexString("#4ADE80");
    public static final TextColor DANGER     = TextColor.fromHexString("#F87171");
    public static final TextColor INFO       = TextColor.fromHexString("#22D3EE");
    public static final TextColor SOFT_WHITE = TextColor.fromHexString("#F1F5F9");
    public static final TextColor MUTED      = TextColor.fromHexString("#94A3B8");
    public static final TextColor DARK_MUTED = TextColor.fromHexString("#64748B");
    public static final TextColor PURPLE     = TextColor.fromHexString("#C084FC");

    // ── PDC keys ──────────────────────────────────────────────────────
    public static final NamespacedKey KEY_TYPE  = new NamespacedKey("sac", "gui_type");
    public static final NamespacedKey KEY_VALUE = new NamespacedKey("sac", "gui_value");
    public static final NamespacedKey KEY_UUID  = new NamespacedKey("sac", "gui_uuid");
    public static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm");

    /** Content slots in a 54-slot chest (border excluded). 28 slots. */
    public static final int[] CONTENT_SLOTS = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43};

    private Menus() {}

    // ── Player resolution ─────────────────────────────────────────────

    public static @Nullable Player resolvePlayer(String nameOrUuid) {
        if (nameOrUuid == null) return null;
        Player p = Bukkit.getPlayer(nameOrUuid);
        if (p != null) return p;
        try {
            UUID uuid = UUID.fromString(nameOrUuid);
            p = Bukkit.getPlayer(uuid);
            if (p != null) return p;
        } catch (IllegalArgumentException ignored) {}
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(nameOrUuid)) return online;
        }
        return null;
    }

    public static @Nullable SacPlayer resolveSacPlayer(String name) {
        if (name == null) return null;
        var pdm = SacAPI.INSTANCE.getPlayerDataManager();
        for (SacPlayer sp : pdm.getEntries()) {
            if (sp.getName().equalsIgnoreCase(name)) return sp;
        }
        try {
            UUID uuid = UUID.fromString(name);
            for (SacPlayer sp : pdm.getEntries()) {
                if (sp.uuid.equals(uuid)) return sp;
            }
        } catch (IllegalArgumentException ignored) {}
        return null;
    }

    // ── Border fill ───────────────────────────────────────────────────

    public static void fillBorder(Inventory inv) {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta pm = pane.getItemMeta();
        pm.setDisplayName(" ");
        pane.setItemMeta(pm);
        for (int i = 0; i < 9; i++) inv.setItem(i, pane.clone());
        for (int i = 45; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, pane.clone());
        }
        for (int row = 1; row < 5; row++) {
            inv.setItem(row * 9, pane.clone());
            inv.setItem(row * 9 + 8, pane.clone());
        }
    }

    // ── PDC helpers ───────────────────────────────────────────────────

    public static @Nullable String getPDC(ItemStack item, NamespacedKey key) {
        var meta = item.getItemMeta();
        return meta != null ? meta.getPersistentDataContainer().get(key, PersistentDataType.STRING) : null;
    }

    public static void setPDC(ItemStack item, NamespacedKey key, String value) {
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
            item.setItemMeta(meta);
        }
    }

    // ── VL helpers ────────────────────────────────────────────────────

    public static int getTotalVL(SacPlayer sp) {
        return (int) sp.checkManager.allChecks.values().stream()
            .mapToDouble(c -> ((Check) c).violations).sum();
    }

    public static TextColor vlColor(double vl) {
        if (vl >= 100) return DANGER;
        if (vl >= 50)  return TextColor.fromHexString("#FBBF24");
        if (vl >= 10)  return HIGHLIGHT;
        return SUCCESS;
    }
}
