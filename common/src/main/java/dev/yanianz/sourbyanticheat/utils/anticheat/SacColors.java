package dev.yanianz.sourbyanticheat.utils.anticheat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Premium color palette and component utilities for SAC commands/GUI.
 * Uses a carefully curated modern dark-theme palette.
 */
public final class SacColors {

    // ── Primary Palette ──────────────────────────────────────────────
    public static final TextColor BRAND      = TextColor.fromHexString("#FF6B35");  // SAC orange
    public static final TextColor BRAND_DARK = TextColor.fromHexString("#CC4400");  // Dark orange
    public static final TextColor ACCENT     = TextColor.fromHexString("#00D4AA");  // Teal accent
    public static final TextColor ACCENT2    = TextColor.fromHexString("#7C4DFF");  // Purple accent
    public static final TextColor HIGHLIGHT  = TextColor.fromHexString("#FFD740");  // Warm yellow

    // ── Semantic Colors ──────────────────────────────────────────────
    public static final TextColor GREEN      = TextColor.fromHexString("#4ADE80");  // Success green
    public static final TextColor RED        = TextColor.fromHexString("#F87171");  // Error/danger red
    public static final TextColor YELLOW     = TextColor.fromHexString("#FBBF24");  // Warning yellow
    public static final TextColor CYAN       = TextColor.fromHexString("#22D3EE");  // Info cyan
    public static final TextColor PURPLE     = TextColor.fromHexString("#C084FC");  // Special purple

    // ── Neutral Colors ───────────────────────────────────────────────
    public static final TextColor WHITE      = TextColor.fromHexString("#F1F5F9");  // Soft white
    public static final TextColor GRAY       = TextColor.fromHexString("#94A3B8");  // Muted gray
    public static final TextColor DARK_GRAY  = TextColor.fromHexString("#64748B");  // Darker gray
    public static final TextColor MUTED      = TextColor.fromHexString("#475569");  // Very muted

    // ── Legacy Aliases (backward compat) ─────────────────────────────
    public static final TextColor GOLD       = BRAND;
    public static final TextColor DARK_RED   = TextColor.fromHexString("#DC2626");

    // ── Unicode Box-Drawing Characters ───────────────────────────────
    public static final String SEPARATOR     = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
    public static final String SEPARATOR_SM  = "━━━━━━━━━━━━━━━━━━━━";
    public static final String BULLET        = "▸";
    public static final String DOT           = "●";
    public static final String ARROW_RIGHT   = "→";
    public static final String DASH          = "—";
    public static final String BAR_FULL      = "█";
    public static final String BAR_EMPTY     = "░";
    public static final String CHECKMARK     = "✔";
    public static final String CROSS         = "✖";
    public static final String STAR          = "★";
    public static final String DIAMOND       = "◆";

    // ── Component Builders ───────────────────────────────────────────

    /** Builds a styled header bar: ━━ TITLE ━━━━━━━━━━━━━━━ */
    public static Component header(String title) {
        return Component.text()
            .append(Component.text("━━ ", MUTED))
            .append(Component.text(title, BRAND, TextDecoration.BOLD))
            .append(Component.text(" " + SEPARATOR_SM, MUTED))
            .build();
    }

    /** Builds a styled sub-header: ▸ TITLE */
    public static Component subHeader(String title) {
        return Component.text()
            .append(Component.text("  " + BULLET + " ", ACCENT))
            .append(Component.text(title, WHITE, TextDecoration.BOLD))
            .build();
    }

    /** Builds a key-value line:    key: value */
    public static Component kv(String key, String value) {
        return kv(key, value, WHITE);
    }

    /** Builds a key-value line:    key: value (colored) */
    public static Component kv(String key, String value, TextColor valueColor) {
        return Component.text()
            .append(Component.text("   " + key, GRAY))
            .append(Component.text("  " + value, valueColor))
            .build();
    }

    /** Builds a status indicator: ● ACTIVE or ✖ DISABLED */
    public static Component statusBadge(String label, boolean active) {
        return Component.text()
            .append(Component.text("   " + (active ? DOT : CROSS) + " ", active ? GREEN : RED))
            .append(Component.text(label, active ? WHITE : DARK_GRAY))
            .append(Component.text(" " + (active ? "ACTIVE" : "OFF"), active ? GREEN : RED))
            .build();
    }

    /** Builds a progress bar: ████████░░░░ 67% */
    public static Component progressBar(double ratio, int width) {
        int filled = (int) (ratio * width);
        int empty = width - filled;
        TextColor barColor = ratio > 0.7 ? RED : ratio > 0.4 ? YELLOW : GREEN;
        return Component.text()
            .append(Component.text(BAR_FULL.repeat(filled), barColor))
            .append(Component.text(BAR_EMPTY.repeat(empty), MUTED))
            .append(Component.text(" " + String.format("%.0f%%", ratio * 100), GRAY))
            .build();
    }

    /** Builds a clickable command entry for help menus */
    public static Component cmdEntry(String cmd, String desc) {
        return Component.text()
            .append(Component.text("   ", MUTED))
            .append(Component.text("/sac " + cmd, ACCENT)
                .clickEvent(ClickEvent.suggestCommand("/sac " + cmd.split(" ")[0]))
                .hoverEvent(HoverEvent.showText(Component.text("Click to run", GRAY))))
            .append(Component.text("  " + DASH + " ", MUTED))
            .append(Component.text(desc, GRAY))
            .build();
    }

    /** Builds a separator line */
    public static Component separator() {
        return Component.text(SEPARATOR, MUTED);
    }

    /** Builds a footer: ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ */
    public static Component footer() {
        return Component.text(SEPARATOR, MUTED);
    }

    /** Builds an empty line (spacer) */
    public static Component spacer() {
        return Component.empty();
    }

    /** VL color based on severity */
    public static TextColor vlColor(double vl) {
        if (vl >= 100) return RED;
        if (vl >= 50) return YELLOW;
        if (vl >= 10) return HIGHLIGHT;
        return GREEN;
    }

    /** Rank label for numbered lists: #1 #2 #3 etc */
    public static Component rank(int position) {
        TextColor color = switch (position) {
            case 1 -> HIGHLIGHT;
            case 2 -> WHITE;
            case 3 -> ACCENT;
            default -> GRAY;
        };
        return Component.text(" #" + position + " ", color);
    }

    private SacColors() {}
}
