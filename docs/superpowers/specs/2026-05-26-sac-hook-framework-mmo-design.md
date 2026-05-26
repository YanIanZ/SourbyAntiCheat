# SAC Soft-Depend Hook Framework + MMO Exemption — Design

Date: 2026-05-26
Branch: `feat/minigame-rewrite`
Status: Approved (design), pending implementation plan

## Goal

Add a reusable soft-depend hook framework and a new "exempt-while-active"
primitive so that when an integrated plugin grants a player an ability/effect
(MMO super-speed, dash, super-break, custom reach), the relevant anticheat
checks are fully exempt for the duration the ability is active — eliminating
false positives without weakening detection elsewhere.

This is **sub-project #1 of 4** from the 2026-05-26 integration request.
Siblings (own specs later): **S** Skyblock, **O** OldCombatMechanics,
**V** cross-version movement/combat hardening. #1 builds the primitive the
others reuse.

Guiding principles: presence-gated (never crash when a plugin is absent),
reuse the existing leniency/profile architecture, config-driven defaults,
disable-not-delete, pure-testable core.

## Background (existing architecture this builds on)

- `Check.flag()` already short-circuits on `LeniencyTracker.active(checkName, uuid)`
  (TTL windows) — see `common/.../checks/Check.java`.
- 7 leniency handlers (`profile/leniency/handlers/*`) are Bukkit listeners
  registered in `SacBukkitLoaderPlugin.onEnable` that call `LeniencyEventBus.fire`.
- `softDepend` is declared in `bukkit/build.gradle.kts` `bukkit { softDepend = listOf(...) }`
  (de.eldoria.plugin-yml).
- TTL leniency is wrong for *stateful* abilities (clear start/end) — hence the
  new ref-counted "active exemption" primitive below.

---

## Component 1 — `ExemptionRegistry` (common; the new primitive)

Ref-counted, per-player set of actively-exempted check names.

```
package dev.yanianz.sourbyanticheat.profile.exempt;

public final class ExemptionRegistry {
    // uuid -> (checkName -> active-count)
    void start(UUID player, String checkName);   // increment
    void stop(UUID player, String checkName);    // decrement, floor 0
    boolean active(UUID player, String checkName);// count > 0
    void clear(UUID player);                      // on quit
}
```

- **Ref-counting** so two overlapping abilities exempting the same check don't
  cancel each other (stop only un-exempts when the last holder stops).
- Thread-safe (`ConcurrentHashMap` + atomic counts) — events fire on main thread
  but checks read from packet threads.
- Lives on `SacAPI` (Lombok `@Getter getExemptionRegistry()`), constructed in
  bootstrap next to `LeniencyTracker`.
- `Check.flag()` adds, right after the existing leniency short-circuit:
  ```java
  var exempt = SacAPI.INSTANCE.getExemptionRegistry();
  if (exempt != null && exempt.active(player.uuid, checkName)) return false;
  ```
  (null-safe for early bootstrap/tests, mirroring the leniency guard.)
- Cleared in the existing player-quit path (`PlayerDataManager`, where
  `AlertManager.handlePlayerQuit` is already called).

**Tests:** start/stop/active ref-counting; overlapping holders; stop-below-zero
floors at 0; clear; unknown player/check returns false. Pure, no SacAPI.

## Component 2 — Hook framework (bukkit)

```
package dev.yanianz.sourbyanticheat.platform.bukkit.hooks;

interface PluginHook {
    String pluginName();                 // e.g. "mcMMO"
    default boolean isPresent() { return Bukkit.getPluginManager().getPlugin(pluginName()) != null; }
    void register(JavaPlugin plugin, ExemptionRegistry exemptions, HookConfig config);
}

final class HookManager {
    // built with the list of all PluginHook impls
    void enablePresent(JavaPlugin plugin, ExemptionRegistry exemptions, HookConfig config);
    //   for each hook: if config.enabled(hook) && hook.isPresent() -> hook.register(...) + log "[SAC] Hooked X"
}
```

- Called once from `SacBukkitLoaderPlugin.onEnable`, after the leniency handlers.
- Each hook registers its own Bukkit listener(s); a missing plugin → hook skipped
  (its API classes are never loaded, so a missing soft-depend never errors).

## Component 3 — `HookConfig` (common) + config keys

New `hooks:` block in `config.yml` (added via ConfigUpdater, version bump):

```yaml
hooks:
  mcmmo:
    enabled: true
    # ability -> checks exempted while active
    super-speed:   [Speed, KillAuraA]
    berserk:       [AutoClicker, FastBreak]
    super-breaker: [FastBreak]
    giga-drill:    [FastBreak]
  mmocore:
    enabled: true
    on-cast:       [Speed, FlightA, Reach, KillAuraA]   # exempt during ability cast
  mythicmobs:
    enabled: true
    mob-buff:      [Speed, FlightA]
  auraskills:
    enabled: true
    speed-buff:    [Speed]
```

`HookConfig` reads these into `Map<String, List<String>>` per hook + an
`enabled(name)` flag. Defaults baked in so it works with no config edits.

## Component 4 — Concrete hooks (bukkit, one class each)

Each is a `PluginHook` + Bukkit `Listener`. `start→stop` drives
`ExemptionRegistry`. Integration via **compileOnly plugin API** (added to
`bukkit/build.gradle.kts` deps + repo); the class only loads when present.

- **`McMMOHook`** — listens `McMMOPlayerAbilityActivateEvent` /
  `McMMOPlayerAbilityDeactivateEvent`; maps the fired ability to its configured
  checks; `start` on activate, `stop` on deactivate. (mcMMO API on
  `repo.maven.apache.org`/`nexus.neetgames.com`.)
- **`MMOCoreHook`** — MythicLib/MMOCore ability cast events; `start` on cast,
  `stop` on cast-end (or a short fallback timer if no end event).
- **`MythicMobsHook`** — mob skill applying a buff to a player; window-based
  `start` + scheduled `stop` (MythicMobs has no per-effect end event).
- **`AuraSkillsHook`** — skill buff apply/expire.

Where a plugin's API is **not on a public maven repo**, that hook is omitted
from this sub-project and tracked as follow-up (documented in the plan) rather
than implemented via reflection. mcMMO is the reference/first hook.

## Build changes (`bukkit/build.gradle.kts`)

- `softDepend += listOf("mcMMO", "MMOCore", "MMOItems", "MythicMobs", "AuraSkills")`.
- `compileOnly(...)` + `maven { url = ... }` repos for each API actually used.
- These are compile-only — not shaded into the jar.

## Data flow

```
ability activate event (plugin) -> Hook listener -> resolve checks from HookConfig
  -> ExemptionRegistry.start(uuid, check) for each
... player attacks/moves -> Check.flag() -> ExemptionRegistry.active? -> skip
ability deactivate/expire -> Hook -> ExemptionRegistry.stop(uuid, check)
player quit -> ExemptionRegistry.clear(uuid)
```

## Error handling

- All hook registration wrapped so one failing hook can't abort others or
  plugin enable (log + continue), mirroring `CommandRegister`'s safety net.
- ExemptionRegistry guards null/unknown lookups (return false / no-op).
- Window-based stops use the existing scheduler; a missed stop self-heals on
  next quit/clear.

## Testing

- `ExemptionRegistryTest` (pure): ref-count start/stop/active/clear, overlap,
  floor-at-zero, unknown lookups.
- `HookConfigTest` (pure): default map present; enabled flags; ability→checks
  parse.
- Hook listeners: runtime-verified on a dev server with each plugin (manual),
  consistent with how leniency handlers are validated. Listener *mapping* logic
  (ability name → checks) extracted to a pure helper where practical and tested.

## Out of scope (this sub-project)

- Skyblock (#S), OldCombatMechanics (#O), version audit (#V) — separate specs.
- Generic potion-effect exemption — Grim already models potion movement;
  exempting there would blind legitimate checks.
- Reflection-based hooks for plugins without a public API artifact.
