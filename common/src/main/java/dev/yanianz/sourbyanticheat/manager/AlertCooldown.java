package dev.yanianz.sourbyanticheat.manager;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-player gate that collapses bursts of the same check's staff alert.
 * One instance per {@code PunishmentManager} (i.e. per player), so keying by
 * check name alone is sufficient. Time is passed in, never read, so it is
 * deterministic under test.
 */
public final class AlertCooldown {

    private final Map<String, Long> lastSent = new HashMap<>();

    /**
     * @return true if an alert for {@code checkName} may be sent now (and
     *         records the send). False if still inside the cooldown window.
     *         A {@code cooldownMs <= 0} disables the gate (always true).
     */
    public boolean shouldSend(String checkName, long nowMs, long cooldownMs) {
        if (cooldownMs <= 0) return true;
        Long prev = lastSent.get(checkName);
        if (prev != null && nowMs - prev < cooldownMs) return false;
        lastSent.put(checkName, nowMs);
        return true;
    }

    public void clear() {
        lastSent.clear();
    }
}
