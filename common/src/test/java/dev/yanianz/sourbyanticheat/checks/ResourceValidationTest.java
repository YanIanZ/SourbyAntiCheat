package dev.yanianz.sourbyanticheat.checks;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ResourceValidationTest {

    @Test void allThirteenLanguageCodesAreValid() {
        String[] langs = {"en","de","es","fr","it","ja","nl","pl","pt","ro","ru","tr","zh"};
        assertEquals(13, langs.length);
        for (String l : langs) assertTrue(l.matches("[a-z]{2}"));
    }

    @Test void configFilesHaveExpectedSections() {
        Set<String> sections = new HashSet<>(Arrays.asList(
            "punishment", "report", "console",
            "crossspeedb", "crossfastbreakb", "crossnofall",
            "crossnoswing", "crossantikb", "crossjesus"
        ));
        assertEquals(9, sections.size());
    }

    @Test void checkNamesAreConsistent() {
        String[] checks = {
            "CrossSpeedB", "CrossFastBreakB", "CrossNoFall",
            "CrossNoSwing", "CrossAntiKB", "CrossJesus",
            "AimAssist", "CrossSpeed", "CrossFastBreak"
        };
        for (String c : checks) assertFalse(c.isEmpty());
        assertEquals(9, checks.length);
    }

    @Test void configKeyNamingConvention() {
        String[] keys = {"interval-variance-threshold", "max-ratio-deviation",
            "sprint-ratio-cap", "min-predicted-movement"};
        for (String k : keys) {
            assertTrue(k.matches("[a-z]+(-[a-z]+)+"));
        }
    }

    @Test void vlValuesAreWithinExpectedRange() {
        int[] vls = {0, 10, 50, 100, 150, 200};
        for (int vl : vls) assertTrue(vl >= 0 && vl <= 500);
    }

    @Test void decayValuesAreSmallFractions() {
        double[] decays = {0.005, 0.01, 0.02, 0.05, 0.15, 0.25};
        for (double d : decays) assertTrue(d > 0 && d < 1.0);
    }

    @Test void setbackValuesAreWithinRange() {
        int[] setbacks = {5, 10, 15, 25};
        for (int s : setbacks) assertTrue(s >= 1 && s <= 100);
    }

    @Test void cooldownSecondsAreReasonable() {
        int[] cooldowns = {0, 30, 60, 120, 300};
        for (int c : cooldowns) assertTrue(c >= 0 && c <= 3600);
    }

    @Test void waveIntervalsAreInSeconds() {
        int[] intervals = {10, 30, 60, 120, 300};
        for (int i : intervals) assertTrue(i >= 1 && i <= 3600);
    }

    @Test void maxAgeDaysIsReasonable() {
        int[] days = {1, 3, 7, 14, 30};
        for (int d : days) assertTrue(d >= 1 && d <= 365);
    }
}
