package dev.yanianz.sourbyanticheat.manager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WavePunishmentViewTest {
    @Test
    void queueViewIsEmptyByDefault() {
        assertNotNull(WavePunishment.queueView());
        assertTrue(WavePunishment.queueView().isEmpty());
    }
}
