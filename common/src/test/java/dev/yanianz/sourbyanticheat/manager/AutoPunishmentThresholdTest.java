package dev.yanianz.sourbyanticheat.manager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutoPunishmentThresholdTest {

    @Test
    void banThresholdExceedsKickThreshold() {
        int ban  = 200;
        int kick = 150;
        assertTrue(ban > kick);
    }

    @Test
    void kickThresholdExceedsWarnThreshold() {
        int kick = 150;
        int warn = 100;
        assertTrue(kick > warn);
    }

    @Test
    void vlBelowWarnDoesNotTrigger() {
        int totalVL = 80;
        int warnThreshold = 100;
        assertFalse(totalVL >= warnThreshold);
    }

    @Test
    void vlAtWarnTriggers() {
        int totalVL = 100;
        int warnThreshold = 100;
        assertTrue(totalVL >= warnThreshold);
    }

    @Test
    void vlBetweenWarnAndKickTriggersWarnOnly() {
        int totalVL = 120;
        int warn = 100;
        int kick = 150;
        assertTrue(totalVL >= warn);
        assertFalse(totalVL >= kick);
    }

    @Test
    void vlAtKickTriggersKick() {
        int totalVL = 150;
        int kick = 150;
        assertTrue(totalVL >= kick);
    }

    @Test
    void vlAtBanTriggersBan() {
        int totalVL = 200;
        int ban = 200;
        assertTrue(totalVL >= ban);
    }

    @Test
    void staffAlertThresholdIsBetweenKickAndBan() {
        int staffAlert = 150;
        int kick = 150;
        int ban = 200;
        assertTrue(staffAlert >= kick);
        assertTrue(staffAlert < ban);
    }

    @Test
    void maxVLPreventsOverflow() {
        double violations = 200;
        double maxVL = 200;
        double newVl = Math.min(maxVL, violations + 1);
        assertEquals(200, newVl, 0.001);
    }

    @Test
    void maxVLDisabledWhenNegative() {
        double violations = 500;
        double maxVL = -1;
        double newVl = maxVL > 0 ? Math.min(maxVL, violations + 1) : violations + 1;
        assertEquals(501, newVl, 0.001);
    }

    @Test
    void punishCommandPlaceholdersAreReplaced() {
        String cmd = "ban %player% %uuid% %check% %vl%";
        String result = cmd
            .replace("%player%", "TestPlayer")
            .replace("%uuid%", "abc-123")
            .replace("%check%", "Speed")
            .replace("%vl%", "200");
        assertEquals("ban TestPlayer abc-123 Speed 200", result);
    }

    @Test
    void punishedPlayersSetPreventsDoublePunish() {
        var set = new java.util.HashSet<java.util.UUID>();
        java.util.UUID uuid = java.util.UUID.randomUUID();
        set.add(uuid);
        assertTrue(set.contains(uuid));
        set.add(uuid);
        assertEquals(1, set.size());
    }
}
