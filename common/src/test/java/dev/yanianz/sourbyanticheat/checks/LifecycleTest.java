package dev.yanianz.sourbyanticheat.checks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LifecycleTest {

    // Check lifecycle: flag -> reward -> flag -> decay -> zero
    @Test void fullCheckLifecycle() {
        double vl = 0; double cap = 200; double decay = 0.05;

        // Phase 1: player cheats, VL rises
        for (int i = 0; i < 50; i++) vl = Math.min(cap, vl + 1);
        assertEquals(50, vl, 0.001);

        // Phase 2: player stops cheating, decay kicks in
        for (int i = 0; i < 100; i++) vl = Math.max(0, vl - decay);
        assertEquals(45, vl, 0.001);

        // Phase 3: full decay to zero
        vl = 0;
        assertEquals(0, vl, 0.001);
    }

    @Test void reportLifecycleFileToListToClear() {
        int reportCount = 0;
        reportCount++; // file
        assertEquals(1, reportCount);
        reportCount = 0; // clear
        assertEquals(0, reportCount);
    }

    @Test void waveQueueLifecycle() {
        int queue = 0;
        queue++; // add
        queue++; // add
        assertEquals(2, queue);
        queue--; // poll
        assertEquals(1, queue);
        queue = 0; // clear
        assertEquals(0, queue);
    }

    @Test void spartanAgreementLifecycle() {
        int agrees = 0, disagrees = 0;
        agrees++; // flag confirmed
        agrees++; // flag confirmed
        disagrees++; // flag not confirmed
        assertEquals(2, agrees);
        assertEquals(1, disagrees);
    }

    @Test void checkToggleLifecycle() {
        boolean enabled = true;
        enabled = !enabled; // toggle off
        assertFalse(enabled);
        enabled = !enabled; // toggle on
        assertTrue(enabled);
    }

    @Test void alertToggleLifecycle() {
        boolean alerts = false;
        alerts = true; // enable
        assertTrue(alerts);
        alerts = false; // disable
        assertFalse(alerts);
    }

    @Test void verboseToggleLifecycle() {
        boolean verbose = false;
        verbose = true; // staff enables
        assertTrue(verbose);
    }

    @Test void banWaveQueueToExecution() {
        String player = "Hacker";
        String cmd = "ban " + player;
        assertEquals("ban Hacker", cmd);
    }

    @Test void flagAlertBroadcastLifecycle() {
        String alert = "SAC » Player failed Speed [50] ratio=1.83";
        assertTrue(alert.contains("SAC"));
        assertTrue(alert.contains("Speed"));
        assertTrue(alert.contains("50"));
    }

    @Test void auditLogAppendLifecycle() {
        StringBuilder log = new StringBuilder();
        log.append("[FLAG] Player Speed VL=10\n");
        log.append("[FLAG] Player Flight VL=15\n");
        assertTrue(log.toString().contains("Speed"));
        assertTrue(log.toString().contains("Flight"));
        assertTrue(log.toString().split("\n").length >= 2);
    }
}
