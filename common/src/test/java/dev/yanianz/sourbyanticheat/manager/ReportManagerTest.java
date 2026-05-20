package dev.yanianz.sourbyanticheat.manager;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReportManagerTest {

    @Test
    void fileReportStoresReport() {
        UUID reporter = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        var r = ReportManager.fileReport(reporter, "Reporter", target, "Target", "cheating");
        assertTrue(r.success());
        assertFalse(ReportManager.getReports(target).isEmpty());
        ReportManager.clearReports(target);
    }

    @Test
    void fileReportReturnsCooldownMessage() {
        UUID reporter = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        ReportManager.fileReport(reporter, "Reporter", target, "Target", "first");
        var r2 = ReportManager.fileReport(reporter, "Reporter", target, "Target", "second");
        assertFalse(r2.success());
        assertTrue(r2.message().toLowerCase().contains("wait"));
        ReportManager.clearReports(target);
    }

    @Test
    void getReportsReturnsEmpty() {
        assertTrue(ReportManager.getReports(UUID.randomUUID()).isEmpty());
    }

    @Test
    void clearReportsRemovesAll() {
        UUID reporter = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        ReportManager.fileReport(reporter, "Reporter", target, "Target", "cheating");
        assertFalse(ReportManager.getAllReports().isEmpty());
        ReportManager.clearReports(target);
        assertTrue(ReportManager.getAllReports().isEmpty());
    }

    @Test
    void multipleReportsForSameTarget() {
        UUID target = UUID.randomUUID();
        ReportManager.fileReport(UUID.randomUUID(), "A", target, "Target", "aimbot");
        ReportManager.fileReport(UUID.randomUUID(), "B", target, "Target", "speed");
        assertTrue(ReportManager.getReports(target).size() >= 2);
        ReportManager.clearReports(target);
    }

    @Test
    void differentTargetsAreSeparated() {
        UUID t1 = UUID.randomUUID();
        UUID t2 = UUID.randomUUID();
        ReportManager.fileReport(UUID.randomUUID(), "R", t1, "T1", "a");
        ReportManager.fileReport(UUID.randomUUID(), "R", t2, "T2", "b");
        assertTrue(ReportManager.getReports(t1).size() >= 1);
        assertTrue(ReportManager.getReports(t2).size() >= 1);
        ReportManager.clearReports(t1);
        ReportManager.clearReports(t2);
    }

    @Test
    void reportContainsReporterName() {
        UUID target = UUID.randomUUID();
        ReportManager.fileReport(UUID.randomUUID(), "Alice", target, "Bob", "speed");
        var reports = ReportManager.getReports(target);
        assertFalse(reports.isEmpty());
        assertEquals("Alice", reports.get(0).reporterName());
        assertEquals("Bob", reports.get(0).targetName());
        assertEquals("speed", reports.get(0).reason());
        ReportManager.clearReports(target);
    }

    @Test
    void clearUnknownTargetDoesNotThrow() {
        assertDoesNotThrow(() -> ReportManager.clearReports(UUID.randomUUID()));
    }
}
