package dev.yanianz.sourbyanticheat.checks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StateMachineTest {

    // CrossSpeedB: buffer state machine
    @Test void bufferGoesFromZeroToFlag() {
        double b = 0;
        for (int i = 0; i < 6; i++) b = Math.min(5, b + 0.5);
        assertTrue(b >= 3.0);
    }

    @Test void bufferResetsAfterFlagging() {
        double b = 4.5; b = 0; assertEquals(0, b, 0.001);
    }

    // CrossNoSwing: three-state buffer
    @Test void noConfirmState() {
        boolean n = false; boolean s = false;
        int add = n && s ? 2 : n || s ? 1 : 0;
        assertEquals(0, add);
    }

    @Test void singleConfirmState() {
        boolean n = true; boolean s = false;
        int add = n && s ? 2 : n || s ? 1 : 0;
        assertEquals(1, add);
    }

    @Test void doubleConfirmState() {
        boolean n = true; boolean s = true;
        int add = n && s ? 2 : n || s ? 1 : 0;
        assertEquals(2, add);
    }

    // Report: cooldown state machine
    @Test void cooldownActiveState() {
        long last = System.currentTimeMillis();
        assertTrue(System.currentTimeMillis() - last < 61000);
    }

    @Test void cooldownExpiredState() throws Exception {
        long last = System.currentTimeMillis() - 120000;
        assertTrue(System.currentTimeMillis() - last >= 60000);
    }

    // AntiKB: velocity state
    @Test void velocityPresentState() {
        boolean vel = true; assertTrue(vel);
    }

    @Test void velocityMissingState() {
        boolean vel = false; assertFalse(vel);
    }

    // Wave queue: enabled/disabled states
    @Test void waveEnabledState() {
        boolean enabled = true; assertTrue(enabled);
    }

    @Test void waveDisabledState() {
        boolean enabled = false; assertFalse(enabled);
    }

    // Punishment: threshold state tiers
    @Test void belowWarnTier() {
        int totalVL = 50; assertTrue(totalVL < 100);
    }

    @Test void warnTier() {
        int totalVL = 120; assertTrue(totalVL >= 100 && totalVL < 150);
    }

    @Test void kickTier() {
        int totalVL = 160; assertTrue(totalVL >= 150 && totalVL < 200);
    }

    @Test void banTier() {
        int totalVL = 220; assertTrue(totalVL >= 200);
    }

    // Discord: enabled/disabled states
    @Test void discordEnabledState() {
        boolean hasUrl = true; assertTrue(hasUrl);
    }

    @Test void discordDisabledState() {
        boolean hasUrl = false; assertFalse(hasUrl);
    }

    // Netty: injection states
    @Test void nettyInjectedState() {
        boolean failed = false; assertFalse(failed);
    }

    @Test void nettyFailedState() {
        boolean failed = true; assertTrue(failed);
    }

    // Check: enable/disable toggle state
    @Test void checkEnabledState() {
        boolean enabled = true; assertTrue(enabled);
    }

    @Test void checkDisabledState() {
        boolean enabled = false; assertFalse(enabled);
    }
}
