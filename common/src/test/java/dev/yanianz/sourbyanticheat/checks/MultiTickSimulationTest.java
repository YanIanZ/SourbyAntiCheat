package dev.yanianz.sourbyanticheat.checks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MultiTickSimulationTest {

    // Simulates CrossSpeedB over 20 ticks — sprinting player should NOT flag
    @Test
    void sprintingPlayerAcrossTwentyTicksNoFlag() {
        double buffer = 0;
        double consistentRatio = 0;
        double sprintCap = 2.4;
        double maxDeviation = 0.5;
        double cap = 5.0;
        int flagCount = 0;

        for (int tick = 0; tick < 20; tick++) {
            double ratio = 1.83; // sprinting player
            double diff = Math.abs(ratio - consistentRatio);

            if (diff < maxDeviation && ratio > sprintCap) {
                buffer = Math.min(cap, buffer + 0.5);
            } else {
                buffer = Math.max(0, buffer - 1.0);
                consistentRatio = ratio;
            }

            if (buffer >= 3.0) flagCount++;
        }
        assertEquals(0, flagCount);
    }

    // Simulates CrossSpeedB — walking player with ratio 1.83, should accumulate
    @Test
    void walkingPlayerWithHighRatioAccumulates() {
        double buffer = 0;
        double consistentRatio = 0;
        double walkThreshold = 1.8;
        double maxDeviation = 0.5;
        double cap = 5.0;

        for (int tick = 0; tick < 20; tick++) {
            double ratio = 1.83;
            double diff = Math.abs(ratio - consistentRatio);

            if (diff < maxDeviation && ratio > walkThreshold) {
                buffer = Math.min(cap, buffer + 0.5);
            } else {
                buffer = Math.max(0, buffer - 1.0);
                consistentRatio = ratio;
            }
        }
        assertEquals(5.0, buffer, 0.001);
    }

    // Simulates CrossNoSwing — 5 attacks with neither netty nor spartan confirming
    @Test
    void noSwingFiveAttacksWithoutConfirmation() {
        int buffer = 0;
        int cap = 5;
        int flags = 0;

        for (int i = 0; i < 5; i++) {
            buffer = Math.max(0, buffer - 1);
            if (buffer >= 4) flags++;
        }
        assertEquals(0, flags);
        assertEquals(0, buffer);
    }

    // Simulates CrossNoSwing — Spartan confirms each time, netty doesn't
    @Test
    void noSwingSpartanConfirmsNettyDoesNot() {
        int buffer = 0;
        int cap = 5;
        int flags = 0;

        for (int i = 0; i < 10; i++) {
            buffer = Math.min(cap, buffer + 1); // one source confirms
            if (buffer >= 4) {
                flags++;
                buffer = 0; // post-flag reset
            }
        }
        assertEquals(2, flags); // flags at 4,8
    }

    // Simulates CrossNoFall — yOffset high but fullOffset low (step-up)
    @Test
    void noFallStepUpNotFlagged() {
        int flags = 0;
        double yOffset = 0.42;
        double fullOffset = 0.0;
        double yThreshold = 0.1;
        double fullThreshold = 0.2;
        boolean onGround = true;

        boolean groundSpoof = onGround && yOffset > yThreshold && fullOffset > fullThreshold;
        if (groundSpoof) flags++;
        assertEquals(0, flags);
    }

    // Simulates CrossAntiKB over 5 knockback events
    @Test
    void antiKbMultipleKnockbackEvents() {
        int buffer = 0;
        int cap = 5;
        int flags = 0;

        for (int i = 0; i < 5; i++) {
            double predicted = 0.3 + i * 0.1;
            double actual = 0.05;
            double ratio = actual / predicted;
            double minKB = 0.15;

            if (predicted >= minKB && ratio < 0.5) {
                buffer = Math.min(cap, buffer + 1);
                if (buffer >= 2) {
                    flags++;
                    buffer = 0;
                }
            }
        }
        assertEquals(2, flags);
    }

    // Simulates CrossFastBreakB — 12 consistent breaks, netty confirms
    @Test
    void fastBreakConsistentWithNettyConfirm() {
        int consistent = 0;
        int gate = 12;
        int buffer = 0;
        boolean nettyConfirms = true;
        boolean spartanConfirms = false;

        for (int i = 0; i < 15; i++) {
            if (consistent < 15) consistent++;
            if (consistent >= gate) {
                if (spartanConfirms && nettyConfirms) {
                    buffer = Math.min(5, buffer + 2);
                } else if (spartanConfirms || nettyConfirms) {
                    buffer = Math.min(5, buffer + 1);
                } else {
                    buffer = Math.max(0, buffer - 1);
                }
            }
        }
        assertTrue(buffer >= 3);
    }

    // Simulates AimAssist snap detection cooldown
    @Test
    void aimAssistCooldownPreventsSpamFlags() {
        int flags = 0;
        int cooldown = 0;
        int snapStreak = 0;
        int threshold = 8;

        for (int tick = 0; tick < 100; tick++) {
            if (cooldown > 0) {
                cooldown--;
                continue;
            }
            snapStreak++;
            if (snapStreak >= threshold) {
                flags++;
                cooldown = 20;
                snapStreak = 0;
            }
        }
        assertTrue(flags >= 2); // multiple flags with cooldown gaps
    }

    // Simulates max VL cap across multiple flags
    @Test
    void maxVlPreventsUnboundedGrowth() {
        double vl = 0;
        double maxVL = 200;

        for (int i = 0; i < 500; i++) {
            vl = maxVL > 0 ? Math.min(maxVL, vl + 1) : vl + 1;
        }
        assertEquals(200, vl, 0.001);
    }

    // Simulates decay working properly
    @Test
    void decayReturnsVlNearZero() {
        double vl = 50;
        double decay = 0.05;

        int ticks = 0;
        while (vl > 1e-10 && ticks < 2000) {
            vl = Math.max(0, vl - decay);
            ticks++;
        }
        assertTrue(ticks >= 990 && ticks <= 1010);
        assertTrue(vl < 0.001);
    }
}
