package dev.yanianz.sourbyanticheat.spartan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpartanCrossCheckTest {

    @Test
    void crossCheckResultTypeEnumHasFourValues() {
        assertEquals(4, SpartanCrossCheck.CrossCheckResult.Type.values().length);
    }

    @Test
    void notAvailableTypeExists() {
        assertNotNull(SpartanCrossCheck.CrossCheckResult.Type.valueOf("NOT_AVAILABLE"));
    }

    @Test
    void notFoundTypeExists() {
        assertNotNull(SpartanCrossCheck.CrossCheckResult.Type.valueOf("NOT_FOUND"));
    }

    @Test
    void spartanCleanTypeExists() {
        assertNotNull(SpartanCrossCheck.CrossCheckResult.Type.valueOf("SPARTAN_CLEAN"));
    }

    @Test
    void spartanFlaggedTypeExists() {
        assertNotNull(SpartanCrossCheck.CrossCheckResult.Type.valueOf("SPARTAN_FLAGGED"));
    }

    @Test
    void spartanFlaggedIsNotEqualToClean() {
        assertNotEquals(
            SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED,
            SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_CLEAN);
    }

    @Test
    void checkSpartanWithNullUuidReturnsNotAvailable() {
        SpartanCrossCheck.CrossCheckResult result = SpartanCrossCheck.checkSpartan(null, "Speed");
        assertNotNull(result);
        assertEquals(SpartanCrossCheck.CrossCheckResult.Type.NOT_AVAILABLE, result.type());
    }

    @Test
    void checkSpartanWithEmptyCheckTypeReturnsNotAvailable() {
        SpartanCrossCheck.CrossCheckResult result = SpartanCrossCheck.checkSpartan(
            java.util.UUID.randomUUID(), "");
        assertNotNull(result);
        assertEquals(SpartanCrossCheck.CrossCheckResult.Type.NOT_AVAILABLE, result.type());
    }

    @Test
    void statsDefaultToZero() {
        var stats = SpartanCrossCheck.getStats(java.util.UUID.randomUUID());
        assertEquals(0, stats.agreements);
        assertEquals(0, stats.disagreements);
    }

    @Test
    void agreementRateIsZeroWhenNoFlags() {
        var stats = SpartanCrossCheck.getStats(java.util.UUID.randomUUID());
        assertEquals(0.0, stats.agreementRate(), 0.001);
    }

    @Test
    void crossCheckResultWithSpartanFlagged() {
        var result = new SpartanCrossCheck.CrossCheckResult(
            SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED, 5);
        assertEquals(SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED, result.type());
        assertEquals(5, result.spartanVL());
    }

    @Test
    void crossCheckResultWithSpartanClean() {
        var result = new SpartanCrossCheck.CrossCheckResult(
            SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_CLEAN, 0);
        assertEquals(SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_CLEAN, result.type());
    }
}
