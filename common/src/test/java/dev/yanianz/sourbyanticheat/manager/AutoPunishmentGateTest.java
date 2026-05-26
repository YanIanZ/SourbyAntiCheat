package dev.yanianz.sourbyanticheat.manager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutoPunishmentGateTest {

    /** Mirrors AutoPunishment.init()'s enable expression. */
    private boolean enabled(boolean legacyFlag, String ban, String kick, String warn) {
        boolean hasAction = !ban.isEmpty() || !kick.isEmpty() || !warn.isEmpty();
        return legacyFlag && hasAction;
    }

    @Test
    void disabledWhenLegacyFlagOffEvenWithCommands() {
        assertFalse(enabled(false, "ban %player%", "", ""));
    }

    @Test
    void disabledWhenLegacyOnButNoActions() {
        assertFalse(enabled(true, "", "", ""));
    }

    @Test
    void enabledWhenLegacyOnAndActionPresent() {
        assertTrue(enabled(true, "", "", "&cwarn"));
    }
}
