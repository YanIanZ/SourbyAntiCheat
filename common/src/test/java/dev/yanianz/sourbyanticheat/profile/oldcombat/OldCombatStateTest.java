package dev.yanianz.sourbyanticheat.profile.oldcombat;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OldCombatStateTest {

    @Test
    void defaultsAreOff() {
        OldCombatState s = new OldCombatState();
        assertFalse(s.cpsRelaxed());
        assertFalse(s.isDisabled("AutoClicker"));
        assertFalse(s.isDisabled(null));
    }

    @Test
    void cpsRelaxedToggles() {
        OldCombatState s = new OldCombatState();
        s.setCpsRelaxed(true);
        assertTrue(s.cpsRelaxed());
    }

    @Test
    void disabledChecksMembership() {
        OldCombatState s = new OldCombatState();
        s.setDisabledChecks(Set.of("AttackFrequency", "Reach"));
        assertTrue(s.isDisabled("AttackFrequency"));
        assertTrue(s.isDisabled("Reach"));
        assertFalse(s.isDisabled("AutoClicker"));
    }

    @Test
    void setDisabledChecksReplaces() {
        OldCombatState s = new OldCombatState();
        s.setDisabledChecks(Set.of("Reach"));
        s.setDisabledChecks(Set.of("AttackFrequency"));
        assertFalse(s.isDisabled("Reach"));
        assertTrue(s.isDisabled("AttackFrequency"));
    }

    @Test
    void nullDisabledSetClearsSafely() {
        OldCombatState s = new OldCombatState();
        s.setDisabledChecks(Set.of("Reach"));
        s.setDisabledChecks(null);
        assertFalse(s.isDisabled("Reach"));
    }
}
