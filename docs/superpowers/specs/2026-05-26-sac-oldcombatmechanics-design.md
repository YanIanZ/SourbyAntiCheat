# SAC OldCombatMechanics Compat — Design

Date: 2026-05-26
Branch: `feat/minigame-rewrite`
Status: Approved (design), pending implementation plan

## Goal

When OldCombatMechanics (OCM, kernitus) restores 1.8 PvP, prevent the one check
that actually false-positives — `AutoClicker`'s raw-CPS path on legit fast 1.8
clicking — while keeping all real detection, plus give operators a config knob to
disable any other checks under OCM.

Sub-project **#3 of 4** (after #1 hooks, #2 skyblock; before #4 version audit).

## Background (what OCM does and does NOT break)

OCM is server-side. SAC's combat checks are packet/velocity-driven, so most
self-adapt:
- **AntiVelocity / AntiKB** read the server's actual velocity packets → adapt to
  OCM `old-knockback`. Not broken.
- **Criticals** is fall-pattern (deltaY) based, not cooldown-gated. Not broken.
- **AttackFrequency** flags physically-impossible rates (>1 attack/tick, <25 ms
  intervals) — impossible even in 1.8. Relaxing it would blind real cheats →
  intentionally NOT relaxed (but operator-disable-able via config).
- **AutoClicker** has two flag paths:
  1. raw CPS `currentCPS > maxLegitCps` (default 30) — **the FP risk**: legit
     butterfly/drag-clicking in cooldown-disabled 1.8 combat can exceed it.
  2. low-variance `currentCPS > cpsFlagThreshold && varianceRange < minLegitVariance`
     — inhuman consistency; humans always have variance → safe to keep.

So OCM compat = relax AutoClicker path (1) only, keep path (2), keep everything
else; plus a config disable-list for operator overrides.

## Components

### `OldCombatState` (common) — on `SacAPI`

```
package dev.yanianz.sourbyanticheat.profile.oldcombat;

public final class OldCombatState {
    void setCpsRelaxed(boolean);        boolean cpsRelaxed();
    void setDisabledChecks(Set<String>);boolean isDisabled(String checkName); // case-sensitive check names
}
```

- Final field on `SacAPI` (`getOldCombatState()`), like `ExemptionRegistry`.
- `cpsRelaxed` volatile; `disabledChecks` an immutable `Set` swapped atomically.
- Defaults: `cpsRelaxed=false`, `disabledChecks=empty` (no-op when OCM absent).

### `Check.flag()` short-circuit

After the exemption short-circuit, add (null-safe):

```java
var oc = SacAPI.INSTANCE.getOldCombatState();
if (oc != null && oc.isDisabled(checkName)) return false;
```

### `AutoClicker` relax

The raw-CPS flag becomes conditional:

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

The variance path is unchanged — inhuman autoclickers are still caught on OCM.

### `OldCombatHook` (bukkit) — presence-gated, no compileOnly

At `onEnable` (with the other hooks):
- If `Bukkit.getPluginManager().getPlugin("OldCombatMechanics") == null` → no-op.
- Read OCM's own config (generic Bukkit, no API dep):
  `getPlugin("OldCombatMechanics").getConfig().getBoolean("disable-attack-cooldown.enabled", false)`
  → `oldCombatState.setCpsRelaxed(...)`.
- Read SAC config `hooks.oldcombatmechanics.disable-when-active` (string list) →
  `oldCombatState.setDisabledChecks(...)`.
- Gated by SAC config `hooks.oldcombatmechanics.enabled` (default true).
- Log what it applied. Wrapped so failure can't abort enable.

### Config (`config.yml`, ConfigUpdater bump)

```yaml
hooks:
    oldcombatmechanics:
        enabled: true
        # Checks to fully disable while OldCombatMechanics is installed.
        # (AutoClicker auto-relaxes its raw-CPS path when disable-attack-cooldown
        #  is on; you usually do NOT need to list it here.)
        disable-when-active: []
```

## Data flow

```
onEnable -> OldCombatHook: OCM present?
  -> read OCM config.yml disable-attack-cooldown.enabled -> OldCombatState.cpsRelaxed
  -> read SAC hooks.oldcombatmechanics.disable-when-active -> OldCombatState.disabledChecks
... AutoClicker raw-CPS path skipped when cpsRelaxed (variance path kept)
... Check.flag() returns false for any check in disabledChecks
```

## Error handling

- Hook wrapped (log + continue); OCM absent → defaults leave all behavior normal.
- `OldCombatState` null-safe; reading OCM's config defaults to false if the key
  is missing (OCM version differences).

## Testing

- `OldCombatStateTest` (pure): cpsRelaxed get/set; disabledChecks set/replace;
  isDisabled membership; defaults (false/empty); null-safe `isDisabled(null)`.
- AutoClicker relax + Check disable-list short-circuit: runtime-verified (depend
  on `SacAPI.INSTANCE`); the decision (`cpsRelaxed`/`isDisabled`) is covered by
  `OldCombatStateTest`.
- Manual smoke on a server with OCM + disable-attack-cooldown: legit fast
  clicking no longer trips AutoClicker raw-CPS; inhuman-consistency autoclick
  still flags.

## Out of scope

- KB/crit prediction adjustments — those checks self-adapt; no change.
- Reading OCM via its API (use its config.yml; no dependency).
- Relaxing AttackFrequency (physically-impossible-rate detection; disable via the
  config list only if the operator insists).
