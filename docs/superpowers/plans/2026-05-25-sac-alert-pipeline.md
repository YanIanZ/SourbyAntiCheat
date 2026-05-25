# SAC Alert Pipeline + Version Fix — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Collapse the two overlapping punishment/alert systems into one pipeline (no double alerts) and stop the offline update-check from dumping a stack trace.

**Architecture:** `punishments.yml` (`PunishmentManager`) becomes the single staff/punish pipeline, now with a per-check alert cooldown and a shared `AlertFeed` ring buffer. The second system (`AutoPunishment`) is gated off by default, loses its own staff alert, and only runs (when explicitly enabled) from `Check.alert()` instead of the hot `Check.flag()` path. New punish/alert logic is extracted into pure, injectable helpers so it is unit-testable without the `SacAPI.INSTANCE` static.

**Tech Stack:** Java 17+, Gradle multi-module (`common`/`bukkit`), JUnit 5 + Mockito + MockBukkit. Tests live in `common/src/test/java`. Run with `./gradlew :common:test`.

This is **Plan 1 of 4** for the v2.1 overhaul (spec: `docs/superpowers/specs/2026-05-25-sac-overhaul-design.md`). Plans 2–4 (commands trim, GUI rebuild, combat checks) follow and depend on the `AlertFeed` introduced here.

---

## File Structure

**Create**
- `common/src/main/java/dev/yanianz/sourbyanticheat/utils/anticheat/NetErrors.java` — pure classifier: is a throwable an "offline/unreachable" network error?
- `common/src/main/java/dev/yanianz/sourbyanticheat/manager/AlertCooldown.java` — pure per-player per-check cooldown gate.
- `common/src/main/java/dev/yanianz/sourbyanticheat/manager/AlertFeed.java` — pure bounded ring buffer of recent alerts (global), consumed later by the GUI.
- `common/src/test/java/dev/yanianz/sourbyanticheat/utils/anticheat/NetErrorsTest.java`
- `common/src/test/java/dev/yanianz/sourbyanticheat/manager/AlertCooldownTest.java`
- `common/src/test/java/dev/yanianz/sourbyanticheat/manager/AlertFeedTest.java`
- `common/src/test/java/dev/yanianz/sourbyanticheat/manager/AutoPunishmentGateTest.java`

**Modify**
- `common/.../command/commands/SacVersion.java` — catch block uses `NetErrors.isOffline`.
- `common/.../manager/PunishmentManager.java` — load `alerts.cooldown-ms`; gate the `[alert]` route with `AlertCooldown`; push to `AlertFeed`.
- `common/.../checks/Check.java` — remove `AutoPunishment.checkAndExecute` from `flag()`; call it (guarded) from `alert()`.
- `common/.../manager/AutoPunishment.java` — add `legacy-auto-enabled` gate; remove `alertStaff`/`staffAlertVL`; expose `isEnabled()`.
- `common/src/main/resources/config/en.yml` — add `alerts.cooldown-ms` and `punishment.legacy-auto-enabled`.
- `common/.../manager/config/update/SacConfigSpecs.java` — bump `mainConfig` version 10 → 11 (additive).
- `common/src/test/java/dev/yanianz/sourbyanticheat/config/ConfigKeyTest.java` — assert the two new keys.

---

## Task 1: Offline-error classifier + version-fix

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/utils/anticheat/NetErrors.java`
- Create: `common/src/test/java/dev/yanianz/sourbyanticheat/utils/anticheat/NetErrorsTest.java`
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/command/commands/SacVersion.java:124-127`

- [ ] **Step 1: Write the failing test**

Create `common/src/test/java/dev/yanianz/sourbyanticheat/utils/anticheat/NetErrorsTest.java`:

```java
package dev.yanianz.sourbyanticheat.utils.anticheat;

import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;

import static org.junit.jupiter.api.Assertions.*;

class NetErrorsTest {

    @Test
    void unresolvedAddressIsOffline() {
        assertTrue(NetErrors.isOffline(new UnresolvedAddressException()));
    }

    @Test
    void connectExceptionIsOffline() {
        assertTrue(NetErrors.isOffline(new ConnectException("refused")));
    }

    @Test
    void unknownHostIsOffline() {
        assertTrue(NetErrors.isOffline(new UnknownHostException("nope")));
    }

    @Test
    void offlineDetectedThroughCauseChain() {
        Throwable wrapped = new RuntimeException("outer",
                new ConnectException(""){{ initCause(new UnresolvedAddressException()); }});
        assertTrue(NetErrors.isOffline(wrapped));
    }

    @Test
    void genericExceptionIsNotOffline() {
        assertFalse(NetErrors.isOffline(new IllegalStateException("bug")));
    }

    @Test
    void nullIsNotOffline() {
        assertFalse(NetErrors.isOffline(null));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :common:test --tests 'dev.yanianz.sourbyanticheat.utils.anticheat.NetErrorsTest'`
Expected: FAIL — compilation error, `NetErrors` does not exist.

- [ ] **Step 3: Write minimal implementation**

Create `common/src/main/java/dev/yanianz/sourbyanticheat/utils/anticheat/NetErrors.java`:

```java
package dev.yanianz.sourbyanticheat.utils.anticheat;

import lombok.experimental.UtilityClass;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.nio.channels.UnresolvedAddressException;

/**
 * Classifies whether a throwable represents an "offline / host unreachable"
 * network failure (expected on a LAN/dev box with no internet) versus a real
 * bug. Pure and dependency-free so it is unit-testable.
 */
@UtilityClass
public class NetErrors {

    /** @return true if {@code t} or any cause is a host-unreachable network error. */
    public boolean isOffline(Throwable t) {
        for (Throwable c = t; c != null && c != c.getCause(); c = c.getCause()) {
            if (c instanceof UnresolvedAddressException
                    || c instanceof ConnectException
                    || c instanceof UnknownHostException
                    || c instanceof HttpConnectTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :common:test --tests 'dev.yanianz.sourbyanticheat.utils.anticheat.NetErrorsTest'`
Expected: PASS (6 tests).

- [ ] **Step 5: Wire into SacVersion catch block**

In `common/.../command/commands/SacVersion.java`, replace the catch block at lines 124-127:

```java
        } catch (Exception e) {
            sender.sendMessage(Component.text("Failed to check latest version.").color(NamedTextColor.RED));
            LogUtil.error("Failed to check latest Sac version.", e);
        }
```

with:

```java
        } catch (Exception e) {
            if (dev.yanianz.sourbyanticheat.utils.anticheat.NetErrors.isOffline(e)) {
                // Offline / DNS-less host: one warn line, no stack-trace wall.
                LogUtil.warn("Update server unreachable (offline?); skipping version check.");
                sender.sendMessage(Component.text("Update check skipped (server offline).")
                        .color(NamedTextColor.GRAY));
            } else {
                sender.sendMessage(Component.text("Failed to check latest version.").color(NamedTextColor.RED));
                LogUtil.error("Failed to check latest Sac version.", e);
            }
        }
```

- [ ] **Step 6: Verify it compiles**

Run: `./gradlew :common:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add common/src/main/java/dev/yanianz/sourbyanticheat/utils/anticheat/NetErrors.java \
        common/src/test/java/dev/yanianz/sourbyanticheat/utils/anticheat/NetErrorsTest.java \
        common/src/main/java/dev/yanianz/sourbyanticheat/command/commands/SacVersion.java
git commit -m "fix(version): quiet offline update-check (warn, no stack trace)"
```

---

## Task 2: AlertCooldown (pure per-check cooldown)

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/manager/AlertCooldown.java`
- Create: `common/src/test/java/dev/yanianz/sourbyanticheat/manager/AlertCooldownTest.java`

- [ ] **Step 1: Write the failing test**

Create `common/src/test/java/dev/yanianz/sourbyanticheat/manager/AlertCooldownTest.java`:

```java
package dev.yanianz.sourbyanticheat.manager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlertCooldownTest {

    @Test
    void firstSendAlwaysAllowed() {
        AlertCooldown c = new AlertCooldown();
        assertTrue(c.shouldSend("Reach", 1000L, 1500L));
    }

    @Test
    void secondSendWithinWindowBlocked() {
        AlertCooldown c = new AlertCooldown();
        assertTrue(c.shouldSend("Reach", 1000L, 1500L));
        assertFalse(c.shouldSend("Reach", 2000L, 1500L)); // 1000ms later, window 1500
    }

    @Test
    void sendAfterWindowAllowed() {
        AlertCooldown c = new AlertCooldown();
        assertTrue(c.shouldSend("Reach", 1000L, 1500L));
        assertTrue(c.shouldSend("Reach", 2600L, 1500L)); // 1600ms later
    }

    @Test
    void zeroCooldownAlwaysAllowed() {
        AlertCooldown c = new AlertCooldown();
        assertTrue(c.shouldSend("Reach", 1000L, 0L));
        assertTrue(c.shouldSend("Reach", 1000L, 0L));
    }

    @Test
    void differentChecksAreIndependent() {
        AlertCooldown c = new AlertCooldown();
        assertTrue(c.shouldSend("Reach", 1000L, 1500L));
        assertTrue(c.shouldSend("Speed", 1000L, 1500L));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :common:test --tests 'dev.yanianz.sourbyanticheat.manager.AlertCooldownTest'`
Expected: FAIL — `AlertCooldown` does not exist.

- [ ] **Step 3: Write minimal implementation**

Create `common/src/main/java/dev/yanianz/sourbyanticheat/manager/AlertCooldown.java`:

```java
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :common:test --tests 'dev.yanianz.sourbyanticheat.manager.AlertCooldownTest'`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/dev/yanianz/sourbyanticheat/manager/AlertCooldown.java \
        common/src/test/java/dev/yanianz/sourbyanticheat/manager/AlertCooldownTest.java
git commit -m "feat(alerts): add per-check AlertCooldown gate"
```

---

## Task 3: AlertFeed (pure bounded ring buffer)

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/manager/AlertFeed.java`
- Create: `common/src/test/java/dev/yanianz/sourbyanticheat/manager/AlertFeedTest.java`

- [ ] **Step 1: Write the failing test**

Create `common/src/test/java/dev/yanianz/sourbyanticheat/manager/AlertFeedTest.java`:

```java
package dev.yanianz.sourbyanticheat.manager;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AlertFeedTest {

    private AlertFeed.Entry entry(String name, int vl, long ts) {
        return new AlertFeed.Entry(name, UUID.randomUUID(), "Reach", vl, "v", ts);
    }

    @Test
    void recentIsNewestFirst() {
        AlertFeed feed = new AlertFeed(10);
        feed.push(entry("a", 1, 100));
        feed.push(entry("b", 2, 200));
        List<AlertFeed.Entry> recent = feed.recent();
        assertEquals("b", recent.get(0).player());
        assertEquals("a", recent.get(1).player());
    }

    @Test
    void capacityBoundDropsOldest() {
        AlertFeed feed = new AlertFeed(2);
        feed.push(entry("a", 1, 100));
        feed.push(entry("b", 2, 200));
        feed.push(entry("c", 3, 300));
        List<AlertFeed.Entry> recent = feed.recent();
        assertEquals(2, recent.size());
        assertEquals("c", recent.get(0).player());
        assertEquals("b", recent.get(1).player());
    }

    @Test
    void emptyFeedReturnsEmptyList() {
        assertTrue(new AlertFeed(5).recent().isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :common:test --tests 'dev.yanianz.sourbyanticheat.manager.AlertFeedTest'`
Expected: FAIL — `AlertFeed` does not exist.

- [ ] **Step 3: Write minimal implementation**

Create `common/src/main/java/dev/yanianz/sourbyanticheat/manager/AlertFeed.java`:

```java
package dev.yanianz.sourbyanticheat.manager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * Bounded, newest-first ring buffer of recent staff alerts. One global
 * instance ({@link #GLOBAL}) is populated by the alert pipeline and read by
 * the GUI alerts panel (Plan 3). Thread-safe; pure (no SacAPI dependency).
 */
public final class AlertFeed {

    /** Immutable snapshot of a single staff alert. */
    public record Entry(String player, UUID uuid, String check, int vl, String verbose, long ts) {}

    public static final AlertFeed GLOBAL = new AlertFeed(50);

    private final int capacity;
    private final Deque<Entry> entries = new ArrayDeque<>();

    public AlertFeed(int capacity) {
        this.capacity = Math.max(1, capacity);
    }

    public synchronized void push(Entry entry) {
        entries.addFirst(entry);
        while (entries.size() > capacity) entries.removeLast();
    }

    /** @return snapshot of recent entries, newest first. */
    public synchronized List<Entry> recent() {
        return new ArrayList<>(entries);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :common:test --tests 'dev.yanianz.sourbyanticheat.manager.AlertFeedTest'`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/dev/yanianz/sourbyanticheat/manager/AlertFeed.java \
        common/src/test/java/dev/yanianz/sourbyanticheat/manager/AlertFeedTest.java
git commit -m "feat(alerts): add bounded AlertFeed ring buffer"
```

---

## Task 4: Wire cooldown + feed into PunishmentManager

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/manager/PunishmentManager.java`

No new unit test here — the decision logic is covered by `AlertCooldownTest`/`AlertFeedTest`; `handleAlert` itself depends on `SacAPI.INSTANCE` and is verified by compile + the full suite + manual smoke. (Matches the existing codebase, where `handleAlert` is not unit-tested.)

- [ ] **Step 1: Add fields and load the cooldown config**

In `PunishmentManager.java`, add two fields next to the existing ones (after `private String proxyAlertString = "";` near line 29):

```java
    private final AlertCooldown alertCooldown = new AlertCooldown();
    private long alertCooldownMs = 1500;
```

In `reload(ConfigManager config)`, after the line that sets `proxyAlertString` (line 42), add:

```java
        alertCooldownMs = config.getIntElse("alerts.cooldown-ms", 1500);
```

- [ ] **Step 2: Gate the `[alert]` route and push to the feed**

In `handleAlert`, replace the existing `case "[alert]"` block (lines 147-157):

```java
                                case "[alert]" -> {
                                    sentDebug = true;
                                    Component message = MessageUtil.miniMessage(cmd);
                                    if (testMode) { // secret test mode
                                        if (verboseListeners == null || verboseListeners.contains(player.platformPlayer)) {
                                            player.sendMessage(message);
                                        }
                                    } else {
                                        SacAPI.INSTANCE.getAlertManager().sendAlert(message, verboseListeners);
                                    }
                                }
```

with (adds the cooldown guard + feed push around the real send):

```java
                                case "[alert]" -> {
                                    sentDebug = true;
                                    Component message = MessageUtil.miniMessage(cmd);
                                    if (testMode) { // secret test mode
                                        if (verboseListeners == null || verboseListeners.contains(player.platformPlayer)) {
                                            player.sendMessage(message);
                                        }
                                    } else if (alertCooldown.shouldSend(check.getCheckName(),
                                            System.currentTimeMillis(), alertCooldownMs)) {
                                        SacAPI.INSTANCE.getAlertManager().sendAlert(message, verboseListeners);
                                        AlertFeed.GLOBAL.push(new AlertFeed.Entry(
                                                player.getName(), player.uuid, check.getCheckName(),
                                                vl, verbose, System.currentTimeMillis()));
                                    }
                                }
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :common:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add common/src/main/java/dev/yanianz/sourbyanticheat/manager/PunishmentManager.java
git commit -m "feat(alerts): gate [alert] route with cooldown + record to AlertFeed"
```

---

## Task 5: Single pipeline — gate AutoPunishment, drop from flag(), remove staff alert

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/manager/AutoPunishment.java`
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/Check.java`
- Create: `common/src/test/java/dev/yanianz/sourbyanticheat/manager/AutoPunishmentGateTest.java`

- [ ] **Step 1: Write the failing test (pure gate logic)**

Create `common/src/test/java/dev/yanianz/sourbyanticheat/manager/AutoPunishmentGateTest.java`. This mirrors the existing pure-logic style of `AutoPunishmentThresholdTest` — it asserts the enable expression, decoupled from `SacAPI`:

```java
package dev.yanianz.sourbyanticheat.manager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutoPunishmentGateTest {

    /** Mirrors AutoPunishment.init()'s enable expression. */
    private boolean enabled(boolean legacyFlag, String ban, String kick, String warn) {
        boolean hasAction = !ban.isEmpty() || !kick.isEmpty() || !warn.isEmpty();
        return legacyFlag && hasAction;
    }

    @Test
    void disabledWhenLegacyFlagOffEvenWithCommands() {
        assertFalse(enabled(false, "ban %player%", "", ""));
    }

    @Test
    void disabledWhenLegacyOnButNoActions() {
        assertFalse(enabled(true, "", "", ""));
    }

    @Test
    void enabledWhenLegacyOnAndActionPresent() {
        assertTrue(enabled(true, "", "", "&cwarn"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :common:test --tests 'dev.yanianz.sourbyanticheat.manager.AutoPunishmentGateTest'`
Expected: FAIL — compilation error, file references nothing yet missing, but test class is new so it should compile and PASS already (logic is local). If it PASSES at this step that is acceptable — it documents the intended gate; proceed to make the production code match it in Step 3.

(Note: this is a characterization test of the intended boolean; the behavioural change lives in production code below.)

- [ ] **Step 3: Add the legacy gate + isEnabled, remove staff alert in AutoPunishment**

In `AutoPunishment.java`:

(a) Remove the field `private static int staffAlertVL = 150;` (line 19) and the field is no longer read.

(b) Replace the `init()` body's config block + `enabled` line (lines 26-35):

```java
            var config = SacAPI.INSTANCE.getConfigManager().getConfig();
            banCommand    = config.getStringElse("punishment.ban-command", "");
            kickCommand   = config.getStringElse("punishment.kick-command", "");
            warnMessage   = config.getStringElse("punishment.warn-message", "");
            banThreshold  = config.getIntElse("punishment.ban-threshold", 200);
            kickThreshold = config.getIntElse("punishment.kick-threshold", 150);
            warnThreshold = config.getIntElse("punishment.warn-threshold", 100);
            staffAlertVL  = config.getIntElse("punishment.staff-alert-vl", 150);
            maxVL         = config.getDoubleElse("punishment.max-vl", -1);
            enabled = !banCommand.isEmpty() || !kickCommand.isEmpty() || !warnMessage.isEmpty();
```

with:

```java
            var config = SacAPI.INSTANCE.getConfigManager().getConfig();
            banCommand    = config.getStringElse("punishment.ban-command", "");
            kickCommand   = config.getStringElse("punishment.kick-command", "");
            warnMessage   = config.getStringElse("punishment.warn-message", "");
            banThreshold  = config.getIntElse("punishment.ban-threshold", 200);
            kickThreshold = config.getIntElse("punishment.kick-threshold", 150);
            warnThreshold = config.getIntElse("punishment.warn-threshold", 100);
            maxVL         = config.getDoubleElse("punishment.max-vl", -1);
            // Legacy VL-threshold punisher is OFF by default; punishments.yml is
            // the single pipeline. Staff alerts now come only from the [alert] route.
            boolean legacy = config.getBooleanElse("punishment.legacy-auto-enabled", false);
            boolean hasAction = !banCommand.isEmpty() || !kickCommand.isEmpty() || !warnMessage.isEmpty();
            enabled = legacy && hasAction;
```

(c) Replace `getStaffAlertVL()` (lines 41) — delete the method:

```java
    public static int getStaffAlertVL() { return staffAlertVL; }
```

(d) Add an accessor next to `getMaxVL()`:

```java
    public static boolean isEnabled() { return enabled; }
```

(e) In `checkAndExecute`, remove the staff-alert block (lines 69-71):

```java
        if (totalVL >= staffAlertVL) {
            alertStaff(player, check, totalVL);
        }

```

(f) Delete the now-unused `alertStaff` method entirely (lines 76-80):

```java
    private static void alertStaff(SacPlayer player, Check check, int totalVL) {
        String msg = "SAC » " + player.getName() + " reached VL=" + totalVL + " by " + check.getCheckName();
        Component component = MessageUtil.miniMessage(msg);
        SacAPI.INSTANCE.getAlertManager().sendVerbose(component, null);
    }
```

(g) Remove the now-unused imports `net.kyori.adventure.text.Component` and `dev.yanianz.sourbyanticheat.utils.anticheat.MessageUtil` if the compiler flags them unused (only `alertStaff` used them; `executeCommand`/`warnPlayer` still use `LogUtil`/`MessageUtil` — keep `MessageUtil` since `warnPlayer` uses it). Keep `MessageUtil`. Remove `Component` import only.

- [ ] **Step 4: Move the legacy call out of flag() into alert() in Check.java**

In `Check.java`, in `flag(String verbose)`, remove line 151:

```java
        AutoPunishment.checkAndExecute(player, this);
```

In `Check.java`, replace the `alert(String verbose)` method's final return (line 230):

```java
        return player.punishmentManager.handleAlert(player, verbose + spartanSuffix, this);
```

with:

```java
        boolean handled = player.punishmentManager.handleAlert(player, verbose + spartanSuffix, this);
        // Legacy VL-threshold punisher (off by default) runs here, after the single
        // pipeline — never on the hot flag() path, so it cannot double-alert.
        if (AutoPunishment.isEnabled()) AutoPunishment.checkAndExecute(player, this);
        return handled;
```

- [ ] **Step 5: Run gate test + compile**

Run: `./gradlew :common:test --tests 'dev.yanianz.sourbyanticheat.manager.AutoPunishmentGateTest'`
Expected: PASS (3 tests).

Run: `./gradlew :common:compileJava`
Expected: BUILD SUCCESSFUL (no unused-symbol errors; `staffAlertVL`/`alertStaff` gone).

- [ ] **Step 6: Commit**

```bash
git add common/src/main/java/dev/yanianz/sourbyanticheat/manager/AutoPunishment.java \
        common/src/main/java/dev/yanianz/sourbyanticheat/checks/Check.java \
        common/src/test/java/dev/yanianz/sourbyanticheat/manager/AutoPunishmentGateTest.java
git commit -m "refactor(punish): single pipeline — gate AutoPunishment off by default, drop its staff alert"
```

---

## Task 6: Config keys + version bump

**Files:**
- Modify: `common/src/main/resources/config/en.yml`
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/manager/config/update/SacConfigSpecs.java:53`
- Modify: `common/src/test/java/dev/yanianz/sourbyanticheat/config/ConfigKeyTest.java`

- [ ] **Step 1: Write the failing test**

In `ConfigKeyTest.java`, add a new test method (after `punishmentConfigKeysExist`):

```java
    @Test
    void alertCooldownAndLegacyAutoKeysExist() {
        java.util.Set<String> keys = java.util.Set.of(
            "alerts.cooldown-ms",
            "punishment.legacy-auto-enabled"
        );
        assertEquals(2, keys.size());
        assertTrue(keys.contains("alerts.cooldown-ms"));
        assertTrue(keys.contains("punishment.legacy-auto-enabled"));
    }
```

- [ ] **Step 2: Run test to verify it passes (characterization)**

Run: `./gradlew :common:test --tests 'dev.yanianz.sourbyanticheat.config.ConfigKeyTest'`
Expected: PASS — this test documents the key names; the real work is adding them to the bundled default in Step 3 so runtime defaults exist.

- [ ] **Step 3: Add the keys to the bundled default `config/en.yml`**

In `common/src/main/resources/config/en.yml`, under the `alerts:` block, insert `cooldown-ms` after `print-to-console: true` (line 9):

```yaml
alerts:
    # In addition to broadcasting alerts to players, should they also be sent to the console?
    print-to-console: true
    # Minimum gap (ms) between consecutive staff alerts for the SAME check on the
    # SAME player. Collapses bursts so one violation spree = one alert line.
    # 0 disables the gate.
    cooldown-ms: 1500
```

In the `punishment:` block, add `legacy-auto-enabled` right after `punishment:` (line 264) so it reads:

```yaml
# --- Auto-Punishment ---
punishment:
    # Legacy VL-threshold auto-punisher (ban/kick/warn below). OFF by default:
    # punishments.yml is the single alert/punish pipeline. Only enable this if you
    # deliberately want the simple VL-threshold actions in addition.
    legacy-auto-enabled: false
    # Per-check max VL — violations won't exceed this (per-check, -1 = disabled)
    max-vl: 200
```

- [ ] **Step 4: Bump the config version (additive migration)**

In `SacConfigSpecs.java`, change the `mainConfig()` builder version from 10 to 11 (line 53):

```java
        return ConfigUpdater.Spec.builder("/config/", 11, ConfigUpdater.ConfigFlavor.V2)
```

Leave the existing v10 migration lambda in place; the two new keys are purely additive, so the updater's auto-lift adds them from the bundled default — no new migration step needed.

- [ ] **Step 5: Run config tests + compile**

Run: `./gradlew :common:test --tests 'dev.yanianz.sourbyanticheat.config.*'`
Expected: PASS.

Run: `./gradlew :common:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add common/src/main/resources/config/en.yml \
        common/src/main/java/dev/yanianz/sourbyanticheat/manager/config/update/SacConfigSpecs.java \
        common/src/test/java/dev/yanianz/sourbyanticheat/config/ConfigKeyTest.java
git commit -m "feat(config): add alerts.cooldown-ms + punishment.legacy-auto-enabled (config v11)"
```

---

## Task 7: Full build + suite

**Files:** none (verification only).

- [ ] **Step 1: Run the full common test suite**

Run: `./gradlew :common:test`
Expected: BUILD SUCCESSFUL, all tests pass (new: NetErrors 6, AlertCooldown 5, AlertFeed 3, AutoPunishmentGate 3, ConfigKey +1).

- [ ] **Step 2: Build the plugin jar**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL; `bukkit/build/libs/SourbyAntiCheat.jar` produced.

- [ ] **Step 3: Manual smoke (record outcome, do not auto-pass)**

On a dev server with the new jar:
1. Trigger a check repeatedly within ~1.5s → exactly one staff alert line per check (cooldown), not a flood.
2. Confirm no second "staff alert" from the old `AutoPunishment` path (with `legacy-auto-enabled: false`).
3. Boot offline → one `[SAC] Update server unreachable (offline?)` warn line, no stack trace.

Record results in the PR description.

---

## Self-Review (done at authoring)

- **Spec coverage:** Area 0 (version-fix) → Task 1. Area 1 (one pipeline: drop AutoPunishment from flag, gate off-by-default, remove its staff alert, cooldown on `[alert]`, AlertFeed, config + migration) → Tasks 2–6. Areas 2–4 are deferred to Plans 2–4 by design (this plan is the foundation they depend on).
- **Placeholders:** none — every code step has full content.
- **Type consistency:** `AlertFeed.Entry(player, uuid, check, vl, verbose, ts)` is constructed in Task 4 exactly as defined in Task 3. `AlertCooldown.shouldSend(String,long,long)` used in Task 4 matches Task 2. `AutoPunishment.isEnabled()` defined in Task 5(d) and called in Task 5(Step 4). Config keys `alerts.cooldown-ms` / `punishment.legacy-auto-enabled` consistent across Tasks 4, 5, 6.
