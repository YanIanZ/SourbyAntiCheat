package dev.yanianz.sourbyanticheat.profile.oldcombat;

import java.util.Set;

/**
 * Global OldCombatMechanics compatibility state, populated at enable from OCM's
 * config. {@code cpsRelaxed} = OCM disable-attack-cooldown is on (AutoClicker
 * skips its raw-CPS path). {@code disabledChecks} = operator-listed checks to
 * fully skip while OCM is installed.
 */
public final class OldCombatState {

    private volatile boolean cpsRelaxed = false;
    private volatile Set<String> disabledChecks = Set.of();

    public void setCpsRelaxed(boolean relaxed) {
        this.cpsRelaxed = relaxed;
    }

    public boolean cpsRelaxed() {
        return cpsRelaxed;
    }

    public void setDisabledChecks(Set<String> checks) {
        this.disabledChecks = checks == null ? Set.of() : Set.copyOf(checks);
    }

    public boolean isDisabled(String checkName) {
        return checkName != null && disabledChecks.contains(checkName);
    }
}
