package dev.yanianz.sourbyanticheat.platform.bukkit.gui.menu;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.UUID;

/** Shared constants and helpers for all SAC menu classes. */
public final class Menus {

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
        pm.displayName(net.kyori.adventure.text.Component.empty());
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
