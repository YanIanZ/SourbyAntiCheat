package dev.yanianz.sourbyanticheat.manager;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReportManagerRecordTest {

    @Test
    void reportRecordStoresAllFields() {
        var report = new ReportManager.Report(
            UUID.randomUUID(), "Alice",
            UUID.randomUUID(), "Bob",
            "speed hacks", 1700000000000L);

        assertEquals("Alice", report.reporterName());
        assertEquals("Bob", report.targetName());
        assertEquals("speed hacks", report.reason());
        assertEquals(1700000000000L, report.timestamp());
    }

    @Test
    void reportResultSuccessHasMessage() {
        var r = new ReportManager.ReportResult(true, "OK");
        assertTrue(r.success());
        assertEquals("OK", r.message());
    }

    @Test
    void reportResultFailureHasMessage() {
        var r = new ReportManager.ReportResult(false, "Wait 30s");
        assertFalse(r.success());
        assertTrue(r.message().contains("30s"));
    }

    @Test
    void getAllReportsReturnsSortedByTimestamp() throws Exception {
        UUID reporter1 = UUID.randomUUID();
        UUID reporter2 = UUID.randomUUID();
        UUID target1 = UUID.randomUUID();
        UUID target2 = UUID.randomUUID();

        ReportManager.fileReport(reporter1, "R1", target1, "T1", "first");
        Thread.sleep(5);
        ReportManager.fileReport(reporter2, "R2", target2, "T2", "second");

        List<ReportManager.Report> all = ReportManager.getAllReports();
        assertTrue(all.size() >= 2);
        // Most recent should be first
        assertEquals("second", all.get(0).reason());

        ReportManager.clearReports(target1);
        ReportManager.clearReports(target2);
    }

    @Test
    void clearUnknownTargetDoesNotThrow() {
        assertDoesNotThrow(() -> ReportManager.clearReports(UUID.randomUUID()));
    }
}
