package dev.yanianz.sourbyanticheat.spartan;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpartanCrossCheckEdgeTest {

    @Test
    void notAvailableResultHasZeroVl() {
        var result = SpartanCrossCheck.CrossCheckResult.NOT_AVAILABLE;
        assertEquals(0, result.spartanVL());
        assertEquals(SpartanCrossCheck.CrossCheckResult.Type.NOT_AVAILABLE, result.type());
    }

    @Test
    void notFoundResultHasZeroVl() {
        var result = SpartanCrossCheck.CrossCheckResult.NOT_FOUND;
        assertEquals(0, result.spartanVL());
    }

    @Test
    void resultEquality() {
        var a = new SpartanCrossCheck.CrossCheckResult(
            SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED, 10);
        var b = new SpartanCrossCheck.CrossCheckResult(
            SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED, 10);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void resultInequalityDifferentType() {
        var a = new SpartanCrossCheck.CrossCheckResult(
            SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED, 10);
        var b = new SpartanCrossCheck.CrossCheckResult(
            SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_CLEAN, 10);
        assertNotEquals(a, b);
    }

    @Test
    void resultInequalityDifferentVl() {
        var a = new SpartanCrossCheck.CrossCheckResult(
            SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED, 10);
        var b = new SpartanCrossCheck.CrossCheckResult(
            SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED, 5);
        assertNotEquals(a, b);
    }

    @Test
    void resultToStringContainsType() {
        var result = new SpartanCrossCheck.CrossCheckResult(
            SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED, 10);
        assertTrue(result.toString().contains("SPARTAN_FLAGGED"));
    }

    @Test
    void statsAgreementRateWithData() {
        var stats = new SpartanCrossCheck.CrossCheckStats();
        stats.agreements = 3;
        stats.disagreements = 1;
        assertEquals(0.75, stats.agreementRate(), 0.001);
    }

    @Test
    void statsAgreementRateAllAgree() {
        var stats = new SpartanCrossCheck.CrossCheckStats();
        stats.agreements = 10;
        stats.disagreements = 0;
        assertEquals(1.0, stats.agreementRate(), 0.001);
    }

    @Test
    void statsAgreementRateAllDisagree() {
        var stats = new SpartanCrossCheck.CrossCheckStats();
        stats.agreements = 0;
        stats.disagreements = 5;
        assertEquals(0.0, stats.agreementRate(), 0.001);
    }

    @Test
    void checkSpartanNullUuidReturnsNotAvailable() {
        var r = SpartanCrossCheck.checkSpartan(null, "Speed");
        assertEquals(SpartanCrossCheck.CrossCheckResult.Type.NOT_AVAILABLE, r.type());
    }

    @Test
    void allCrossCheckTypesAreUnique() {
        var types = SpartanCrossCheck.CrossCheckResult.Type.values();
        assertEquals(4, java.util.stream.Stream.of(types).distinct().count());
    }

    @Test
    void typeValueOfRoundTrips() {
        for (var t : SpartanCrossCheck.CrossCheckResult.Type.values()) {
            assertEquals(t, SpartanCrossCheck.CrossCheckResult.Type.valueOf(t.name()));
        }
    }
}
