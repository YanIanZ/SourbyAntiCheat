# SAC OldCombatMechanics Compat Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: executing-plans / subagent-driven-development. Checkbox steps.

**Goal:** Under OCM, relax AutoClicker's raw-CPS path (keep variance detection) + a config disable-list for other checks; everything else self-adapts.

**Architecture:** `OldCombatState` (on SacAPI) holds a `cpsRelaxed` flag + `disabledChecks` set. `OldCombatHook` (presence-gated, reads OCM's config.yml — no compileOnly) populates it at onEnable. `AutoClicker` skips its raw-CPS flag when relaxed; `Check.flag()` short-circuits checks in the disable-list.

**Tech Stack:** Java 17+, Bukkit (`bukkit`), JUnit5 (`common`). Spec: `docs/superpowers/specs/2026-05-26-sac-oldcombatmechanics-design.md`. Sub-project 3 of 4.

**Lombok note:** ignore IDE diagnostics; trust `./gradlew`.

---

## Task 1: OldCombatState (common, pure)

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/profile/oldcombat/OldCombatState.java`
- Create: `common/src/test/java/dev/yanianz/sourbyanticheat/profile/oldcombat/OldCombatStateTest.java`

- [ ] **Step 1: Failing test**

```java
package dev.yanianz.sourbyanticheat.profile.oldcombat;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OldCombatStateTest {

    @Test
    void defaultsAreOff() {
        OldCombatState s = new OldCombatState();
        assertFalse(s.cpsRelaxed());
        assertFalse(s.isDisabled("AutoClicker"));
        assertFalse(s.isDisabled(null));
    }

    @Test
    void cpsRelaxedToggles() {
        OldCombatState s = new OldCombatState();
        s.setCpsRelaxed(true);
        assertTrue(s.cpsRelaxed());
    }

    @Test
    void disabledChecksMembership() {
        OldCombatState s = new OldCombatState();
        s.setDisabledChecks(Set.of("AttackFrequency", "Reach"));
        assertTrue(s.isDisabled("AttackFrequency"));
        assertTrue(s.isDisabled("Reach"));
        assertFalse(s.isDisabled("AutoClicker"));
    }

    @Test
    void setDisabledChecksReplaces() {
        OldCombatState s = new OldCombatState();
        s.setDisabledChecks(Set.of("Reach"));
        s.setDisabledChecks(Set.of("AttackFrequency"));
        assertFalse(s.isDisabled("Reach"));
        assertTrue(s.isDisabled("AttackFrequency"));
    }

    @Test
    void nullDisabledSetClearsSafely() {
        OldCombatState s = new OldCombatState();
        s.setDisabledChecks(Set.of("Reach"));
        s.setDisabledChecks(null);
        assertFalse(s.isDisabled("Reach"));
    }
}
```

- [ ] **Step 2: Run — expect FAIL** (`./gradlew :common:test --tests '*OldCombatStateTest'`)

- [ ] **Step 3: Implement**

```java
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
```

- [ ] **Step 4: Run — expect PASS (5 tests)**
- [ ] **Step 5: Commit** `feat(oldcombat): add OldCombatState (cps-relax + disable-list)`

---

## Task 2: Wire into SacAPI + Check + AutoClicker

**Files:**
- Modify: `common/.../SacAPI.java`
- Modify: `common/.../checks/Check.java`
- Modify: `common/.../checks/impl/combat/AutoClicker.java`

- [ ] **Step 1: SacAPI field + getter** — near the `exemptionRegistry` field, add:

```java
    private final dev.yanianz.sourbyanticheat.profile.oldcombat.OldCombatState oldCombatState =
            new dev.yanianz.sourbyanticheat.profile.oldcombat.OldCombatState();

    public dev.yanianz.sourbyanticheat.profile.oldcombat.OldCombatState getOldCombatState() {
        return oldCombatState;
    }
```

- [ ] **Step 2: Check.flag() short-circuit** — after the ExemptionRegistry block in `flag(String verbose)`, add:

```java
        try {
            var oc = SacAPI.INSTANCE.getOldCombatState();
            if (oc != null && oc.isDisabled(checkName)) return false;
        } catch (Throwable ignored) {}
```

- [ ] **Step 3: AutoClicker relax** — in `AutoClicker.onPacketReceive`, replace the flag block:

```java
            if (currentCPS > maxLegitCps) {
                flagAndAlert("cps=" + currentCPS);
            } else if (currentCPS > cpsFlagThreshold && varianceRange < minLegitVariance && sampleSize >= 10) {
                flagAndAlert("cps=" + currentCPS + " consistent=" + varianceRange + "ms");
            } else {
                reward();
            }
```

with:

```java
            boolean relaxed = SacAPI.INSTANCE.getOldCombatState() != null
                    && SacAPI.INSTANCE.getOldCombatState().cpsRelaxed();
            if (currentCPS > maxLegitCps && !relaxed) {
                flagAndAlert("cps=" + currentCPS);
            } else if (currentCPS > cpsFlagThreshold && varianceRange < minLegitVariance && sampleSize >= 10) {
                flagAndAlert("cps=" + currentCPS + " consistent=" + varianceRange + "ms");
            } else {
                reward();
            }
```

Add `import dev.yanianz.sourbyanticheat.SacAPI;` to AutoClicker if not present.

- [ ] **Step 4: Compile** `./gradlew :common:compileJava` — SUCCESS.
- [ ] **Step 5: Commit** `feat(oldcombat): SacAPI state + Check disable-list + AutoClicker cps-relax`

---

## Task 3: OldCombatHook (bukkit)

**Files:**
- Create: `bukkit/.../platform/bukkit/hooks/OldCombatHook.java`

- [ ] **Step 1: Implement**

```java
package dev.yanianz.sourbyanticheat.platform.bukkit.hooks;

import dev.yanianz.sourbyanticheat.profile.oldcombat.OldCombatState;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * OldCombatMechanics compatibility. Presence-gated; reads OCM's own config.yml
 * (generic Bukkit, no API dependency) to learn if disable-attack-cooldown is on,
 * and applies the operator's disable-list. Populates {@link OldCombatState}.
 */
public final class OldCombatHook {

    private OldCombatHook() {}

    public static final String PLUGIN_NAME = "OldCombatMechanics";

    /**
     * @param disableList operator-configured checks to disable while OCM is present
     */
    public static void apply(OldCombatState state, List<String> disableList) {
        if (state == null) return;
        try {
            Plugin ocm = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
            if (ocm == null) return;

            boolean cooldownDisabled = ocm.getConfig().getBoolean("disable-attack-cooldown.enabled", false);
            state.setCpsRelaxed(cooldownDisabled);

            Set<String> disabled = new LinkedHashSet<>(disableList == null ? List.of() : disableList);
            state.setDisabledChecks(disabled);

            LogUtil.info("OldCombatMechanics detected: cps-relax=" + cooldownDisabled
                    + ", disabled=" + disabled);
        } catch (Throwable t) {
            LogUtil.warn("OldCombatMechanics hook failed: " + t);
        }
    }
}
```

- [ ] **Step 2: Compile** `./gradlew :bukkit:compileJava` — SUCCESS.
- [ ] **Step 3: Commit** `feat(oldcombat): OldCombatHook (reads OCM config, no compileOnly)`

---

## Task 4: Config + wiring + build

**Files:**
- Modify: `common/src/main/resources/config/en.yml` (+ version bump)
- Modify: `common/.../manager/config/update/SacConfigSpecs.java`
- Modify: `bukkit/.../platform/bukkit/SacBukkitLoaderPlugin.java`

- [ ] **Step 1: Config** — under the existing `hooks:` block in `en.yml`, add:

```yaml
    oldcombatmechanics:
        enabled: true
        # Checks to fully disable while OldCombatMechanics is installed.
        # (AutoClicker auto-relaxes its raw-CPS path when disable-attack-cooldown
        #  is on; you usually do NOT need to list it here.)
        disable-when-active: []
```

Bump `config-version: 13` -> `14`. Bump `SacConfigSpecs.mainConfig()` builder 13 -> 14.

- [ ] **Step 2: Wire in onEnable** — in `SacBukkitLoaderPlugin.onEnable`, right after the existing `// --- Soft-depend plugin hooks ---` try/catch block (after the `hookManager.enablePresent(...)` block's closing catch), add:

```java
            // --- OldCombatMechanics compatibility ---
            try {
                var ocCfg = dev.yanianz.sourbyanticheat.SacAPI.INSTANCE.getConfigManager().getConfig();
                if (ocCfg.getBooleanElse("hooks.oldcombatmechanics.enabled", true)) {
                    dev.yanianz.sourbyanticheat.platform.bukkit.hooks.OldCombatHook.apply(
                        dev.yanianz.sourbyanticheat.SacAPI.INSTANCE.getOldCombatState(),
                        ocCfg.getStringListElse("hooks.oldcombatmechanics.disable-when-active", java.util.List.of()));
                }
            } catch (Throwable t) {
                dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil.warn("Failed to init OCM hook: " + t);
            }
```

- [ ] **Step 3: Build + test** `./gradlew build` + `./gradlew :common:test` — both SUCCESS.
- [ ] **Step 4: Commit** `feat(oldcombat): config + onEnable wiring (config v14)`

---

## Self-Review (authoring)
- **Spec coverage:** OldCombatState (Task 1); SacAPI + Check disable-list + AutoClicker relax (Task 2); OldCombatHook reading OCM config (Task 3); config + wiring (Task 4). KB/crit unchanged (self-adapt); AttackFrequency not relaxed (impossible-rate) — both per spec.
- **Placeholders:** none.
- **Type consistency:** `OldCombatState.cpsRelaxed()/setCpsRelaxed/isDisabled/setDisabledChecks` consistent across Tasks 1,2,3. `OldCombatHook.apply(OldCombatState, List<String>)` defined Task 3, called Task 4. `SacAPI.getOldCombatState()` defined Task 2, used Tasks 2,3-via-wiring,4. Config keys `hooks.oldcombatmechanics.enabled` / `.disable-when-active` consistent Tasks 1(test n/a),3,4.
