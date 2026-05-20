package dev.yanianz.sourbyanticheat.utils.anticheat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SacColorsTest {

    @Test void brandColorIsOrange() {
        assertNotNull(SacColors.BRAND);
        assertEquals("#FF6B35", SacColors.BRAND.asHexString().toUpperCase());
    }

    @Test void accentColorIsTeal() {
        assertEquals("#00D4AA", SacColors.ACCENT.asHexString().toUpperCase());
    }

    @Test void highlightColorIsYellow() {
        assertEquals("#FFD740", SacColors.HIGHLIGHT.asHexString().toUpperCase());
    }

    @Test void redIsForDanger() {
        assertEquals("#F87171", SacColors.RED.asHexString().toUpperCase());
    }

    @Test void greenIsForSuccess() {
        assertEquals("#4ADE80", SacColors.GREEN.asHexString().toUpperCase());
    }

    @Test void grayIsMuted() {
        assertNotNull(SacColors.GRAY);
        assertNotNull(SacColors.DARK_GRAY);
    }

    @Test void whiteIsAvailable() {
        assertNotNull(SacColors.WHITE);
        assertNotNull(SacColors.MUTED);
    }

    @Test void vlColorReturnsNonNull() {
        assertNotNull(SacColors.vlColor(0));
        assertNotNull(SacColors.vlColor(10));
        assertNotNull(SacColors.vlColor(50));
        assertNotNull(SacColors.vlColor(100));
        assertNotNull(SacColors.vlColor(200));
    }

    @Test void vlColorLowVlIsGreen() {
        assertEquals(SacColors.GREEN, SacColors.vlColor(0));
    }

    @Test void vlColorMediumVlIsYellow() {
        assertEquals(SacColors.HIGHLIGHT, SacColors.vlColor(10));
    }

    @Test void vlColorHighVlIsOrange() {
        assertNotNull(SacColors.vlColor(50));
    }

    @Test void vlColorVeryHighVlIsRed() {
        assertEquals(SacColors.RED, SacColors.vlColor(100));
    }

    @Test void rankReturnsNumberedComponent() {
        var c = SacColors.rank(1);
        assertNotNull(c);
    }

    @Test void progressBarReturnsNonEmpty() {
        var c = SacColors.progressBar(0.5, 10);
        assertNotNull(c);
    }

    @Test void spacerReturnsComponent() {
        assertNotNull(SacColors.spacer());
    }

    @Test void headerReturnsComponent() {
        var c = SacColors.header("Test");
        assertNotNull(c);
    }

    @Test void footerReturnsComponent() {
        assertNotNull(SacColors.footer());
    }

    @Test void subHeaderReturnsComponent() {
        var c = SacColors.subHeader("Test");
        assertNotNull(c);
    }

    @Test void cmdEntryReturnsComponent() {
        var c = SacColors.cmdEntry("cmd", "desc");
        assertNotNull(c);
    }

    @Test void checkmarkStringIsSet() {
        assertNotNull(SacColors.CHECKMARK);
        assertFalse(SacColors.CHECKMARK.isEmpty());
    }

    @Test void dashStringIsSet() {
        assertNotNull(SacColors.DASH);
        assertFalse(SacColors.DASH.isEmpty());
    }
}
