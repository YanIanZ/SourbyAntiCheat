package dev.yanianz.sourbyanticheat.manager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertCooldownTest {

    @Test
    void firstSendAlwaysAllowed() {
        AlertCooldown c = new AlertCooldown();
        assertTrue(c.shouldSend("Reach", 1000L, 1500L));
    }

    @Test
    void secondSendWithinWindowBlocked() {
        AlertCooldown c = new AlertCooldown();
        assertTrue(c.shouldSend("Reach", 1000L, 1500L));
        assertFalse(c.shouldSend("Reach", 2000L, 1500L)); // 1000ms later, window 1500
    }

    @Test
    void sendAfterWindowAllowed() {
        AlertCooldown c = new AlertCooldown();
        assertTrue(c.shouldSend("Reach", 1000L, 1500L));
        assertTrue(c.shouldSend("Reach", 2600L, 1500L)); // 1600ms later
    }

    @Test
    void zeroCooldownAlwaysAllowed() {
        AlertCooldown c = new AlertCooldown();
        assertTrue(c.shouldSend("Reach", 1000L, 0L));
        assertTrue(c.shouldSend("Reach", 1000L, 0L));
    }

    @Test
    void differentChecksAreIndependent() {
        AlertCooldown c = new AlertCooldown();
        assertTrue(c.shouldSend("Reach", 1000L, 1500L));
        assertTrue(c.shouldSend("Speed", 1000L, 1500L));
    }
}
