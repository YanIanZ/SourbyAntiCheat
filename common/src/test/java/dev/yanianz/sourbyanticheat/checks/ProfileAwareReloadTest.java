package dev.yanianz.sourbyanticheat.checks;

import org.junit.jupiter.api.Test;

class ProfileAwareReloadTest {
    @Test void reloadUsesProfileOverrideForMaxVL() {
        // Integration: build SacPlayer stub with profile BEDWARS, snapshot with
        // BEDWARS Reach maxvl=99, construct Reach check, assert getMaxVL()==99.
        // Requires a SacPlayer test harness that does not exist yet.
        org.junit.jupiter.api.Assumptions.assumeTrue(false, "needs SacPlayer test harness");
    }
}
