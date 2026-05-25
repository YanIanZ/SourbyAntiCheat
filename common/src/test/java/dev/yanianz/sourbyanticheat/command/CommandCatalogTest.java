package dev.yanianz.sourbyanticheat.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandCatalogTest {

    @Test
    void legacyContainsDiagnosticAndOneOffCommands() {
        assertTrue(CommandCatalog.isLegacy("perf"));
        assertTrue(CommandCatalog.isLegacy("debug"));
        assertTrue(CommandCatalog.isLegacy("dump"));
        assertTrue(CommandCatalog.isLegacy("spartan"));
        assertTrue(CommandCatalog.isLegacy("history-migrate"));
        assertTrue(CommandCatalog.isLegacy("checks"));
    }

    @Test
    void coreCommandsAreNotLegacy() {
        assertFalse(CommandCatalog.isLegacy("alerts"));
        assertFalse(CommandCatalog.isLegacy("gui"));
        assertFalse(CommandCatalog.isLegacy("profile"));
        assertFalse(CommandCatalog.isLegacy("reload"));
        assertFalse(CommandCatalog.isLegacy("version"));
    }

    @Test
    void legacySetHasExpectedSize() {
        assertEquals(11, CommandCatalog.LEGACY.size());
    }

    @Test
    void isLegacyIsNullSafe() {
        assertFalse(CommandCatalog.isLegacy(null));
    }
}
