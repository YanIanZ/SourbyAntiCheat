# SAC Overhaul (v2.1) — Design

Date: 2026-05-25
Branch: `feat/minigame-rewrite`
Status: Approved (design), pending implementation plan

## Goal

Five related improvements to SourbyAntiCheat, scoped for a minigame-focused server
and biased toward maintainability:

0. Quiet the offline update-check stack trace.
1. Collapse two overlapping punishment/alert systems into one pipeline (no double alerts).
2. Trim the command surface to a minigame core (disable, not delete, the legacy ones).
3. Rebuild the GUI on holder-based routing and add Profile + Alerts panels.
4. Add conservative combat checks: KillAura + Aim subchecks, strengthen Reach.

Guiding principles: **disable-not-delete** (matches v2.0.0), profile- and
leniency-aware by default, conservative thresholds, tests per unit.

---

## Area 0 — Version-check error

**Problem.** `SacVersion.checkForUpdates` (`common/.../command/commands/SacVersion.java:124`)
catches every exception and logs a full stack trace via `LogUtil.error`. On an
offline / DNS-less host the Modrinth call throws `UnresolvedAddressException`
(wrapped in `ConnectException`). `UpdateChecker.start` runs the check on delayed
init → a stack-trace wall every boot. Pure noise.

**Change.**
- Add a helper that walks the cause chain looking for offline-class exceptions:
  `java.nio.channels.UnresolvedAddressException`, `java.net.ConnectException`,
  `java.net.http.HttpConnectTimeoutException`, `java.net.UnknownHostException`.
- If offline: log a single `LogUtil.warn("Update server unreachable (offline?); skipping version check.")`,
  **no stack trace**. On the auto/console path, suppress the red
  "Failed to check latest version." chat line (still send a short line to an
  interactive player who ran `/sac version`).
- Any other (unexpected) exception keeps the existing trace — that signals a real bug.

**Config.** None. The existing `check-for-updates: true` toggle is unchanged.

**Files.** `SacVersion.java` (catch block + small offline-detect helper).

**Tests.** Unit test on the offline-detect helper (chain of wrapped causes →
classified offline vs. not). No network in tests.

---

## Area 1 — One alert / punishment pipeline

**Problem.** Two systems fire per violation:

- `PunishmentManager` (driven by `punishments.yml`: groups, `[alert]`/`[webhook]`/`[log]`/`[proxy]`
  + threshold/interval commands). Runs inside `Check.alert()`.
- `AutoPunishment` (driven by `config.yml` `punishment.*`: ban/kick/warn-by-VL +
  `staff-alert-vl`). Runs inside `Check.flag()`.

All 220 check call sites use `flagAndAlert`, so both run for every violation.
Worse, `AutoPunishment.alertStaff` calls `sendVerbose` on **every** flag once
`totalVL >= staff-alert-vl` — no interval, no dedup. That is the visible
double/triple alert. The ban/kick/warn-by-VL behaviour also overlaps
punishments.yml command groups.

**Decision (chosen).** `punishments.yml` is the single source of truth.
`AutoPunishment` becomes off-by-default and loses its own staff alert; staff
alerts come only from the punishments.yml `[alert]` route, now with a cooldown.

**Changes.**
- `Check.flag()`: keep `player.punishmentManager.handleViolation(this)` (VL
  bookkeeping). **Remove** the unconditional `AutoPunishment.checkAndExecute(player, this)`
  call from the hot path.
- `Check.alert()`: unchanged as the single staff/punish entry point
  (`PunishmentManager.handleAlert`).
- `AutoPunishment`:
  - New gate `punishment.legacy-auto-enabled` (**default `false`**). `enabled`
    requires this flag in addition to the existing command-present check.
  - **Remove** `alertStaff` and its `sendVerbose` call entirely.
  - When enabled, it is invoked from `alert()` (one place), not `flag()`, so it
    never runs ahead of the dedup.
  - Class is retained (disable-not-delete).
- `PunishmentManager.handleAlert`: before dispatching the `[alert]` route, apply a
  per-`(uuid, checkName)` cooldown. New config `alerts.cooldown-ms` (default `1500`).
  Cooldown state: a small map keyed by uuid+check → last-sent millis, pruned on
  player quit. `[webhook]`/`[log]`/`[proxy]`/commands keep their existing
  threshold/interval semantics (unaffected).
- New `AlertFeed`: in-memory ring buffer (last ~50 entries: player, uuid, check,
  vl, verbose, ts). The `[alert]` route pushes into it. Consumed by the GUI alerts
  panel (Area 3). Lives in `common` so all platforms can populate it; GUI reads it.

**Config / migration.**
- `config.yml`: add `punishment.legacy-auto-enabled: false`, `alerts.cooldown-ms: 1500`.
- `ConfigUpdater` / `SacConfigSpecs`: register the new keys with defaults so
  existing configs migrate cleanly. No existing key is removed.

**Files.** `Check.java`, `AutoPunishment.java`, `PunishmentManager.java`, new
`AlertFeed.java`, config spec + `config/en.yml` (+ other locales as the existing
updater handles), `ConfigUpdater`/`SacConfigSpecs`.

**Tests.**
- Cooldown: two flags of the same check within `cooldown-ms` → one `[alert]` send.
- Regression: a single violation produces exactly one staff-facing alert (no
  `AutoPunishment` second path).
- `AlertFeed` ring-buffer bound + ordering.

---

## Area 2 — Commands: trim to minigame core

**Mechanism.** `CommandRegister.start()` calls `CommandService.registerCommands()`,
which registers a curated list of `BuildableCommand`s (Cloud framework).

**Changes.**
- Define an explicit **core set**, always registered:
  `alerts, verbose, brands, gui, profile, info, list, top, status, reset, exempt,
  toggle, reload, version, help`, plus `report` / `reports` (the GUI depends on them).
- **Legacy set**: `history-migrate, history-copy, dump, spartan, sendalert,
  testwebhook, log, summary, perf, debug, checks`. Classes are **kept**; registered
  only when `commands.legacy-enabled: true` (default `false`). Disable-not-delete.
- `BuildableCommand` gains `default String description()` and
  `default String category()` for help grouping (defaults keep existing commands
  compiling).
- `SacHelp` rewritten: a single grouped page that lists **only registered**
  commands, grouped by category. Full Cloud tab-completion retained.
- Permissions normalised to `sac.command.<sub>` (existing per-command perms kept
  as aliases where already shipped, to avoid breaking operator setups).

**Config.** `commands.legacy-enabled: false` (new, via ConfigUpdater).

**Files.** `CommandService` impl (curated list + legacy gate), `BuildableCommand.java`,
`SacHelp.java`, config spec.

**Tests.**
- Core registry returns the expected set; legacy excluded when flag off, included
  when on.
- `SacHelp` output contains only registered commands.

---

## Area 3 — GUI: holder-based rebuild + new panels

**Problem.** `SacGUI` (`bukkit/.../gui/SacGUI.java`, 522 lines) routes clicks by
matching translated inventory **title strings** (`title.contains("— Checks")`),
using the deprecated `getView().getTitle()`. Fragile and not localizable.

**Changes.**
- Introduce a `SacMenu` base (Bukkit `InventoryHolder`) that carries its own state
  (target player, page index) and exposes `Inventory build()` + `void onClick(Player, InventoryClickEvent)`.
- One `MenuListener` routes: `event.getInventory().getHolder() instanceof SacMenu menu`
  → `menu.onClick(...)`. No title parsing anywhere.
- `Menus` helper: pagination (next/prev/border) + shared item builder reusing the
  existing colour palette and PDC keys.
- Panels:
  - **Hub** — entry buttons + system status.
  - **Players** (paginated) — heads → Player detail.
  - **Player / Checks** — per-check VL, enable/disable toggle, reset-all, Spartan
    cross-check stats (parity with current).
  - **Profiles** — list profiles from `ProfileRegistry`/`ProfileConfig`; per arena:
    view thresholds, toggle disabled-checks, ± adjust a small set of thresholds,
    persisted back to `profiles.yml`.
  - **Alerts feed** — recent flags from `AlertFeed` (Area 1); click → player detail.
  - **Reports** — parity with current.
  - **Wave** — show the real `WavePunishment` queue (not the current static text).
- Delete `SacGUI` only after a parity checklist passes.

**Files.** New package `bukkit/.../gui/menu/` (`SacMenu`, `MenuListener`, `Menus`,
one class per panel). Remove `SacGUI.java` at the end. `SacGUICommand` opens the Hub.

**Tests.** Holder-routing unit test (route resolves by holder type, not title);
profile-edit persistence test (toggle/adjust writes back to a temp profiles.yml).
GUI render itself stays manual-smoke (Bukkit runtime).

---

## Area 4 — New combat checks

All extend `Check`, so they inherit profile resolution, the leniency
short-circuit, and the new alert dedup for free. Each gets `@CheckData`
(name, configName, decay, setback, description, stableKey), config defaults, and a
`checks/en.yml` description entry. Registered in the appropriate `CheckManager`
builder.

- **KillAura** (new, `checks/impl/combat/`, `PacketCheck` on `INTERACT_ENTITY`):
  - `KillAuraA` — attack angle / FOV: flag attacks on entities outside a sane
    look cone (e.g. behind the player).
  - `KillAuraB` — multi-target: distinct entities hit within a tight tick window.
  - `KillAuraC` — attack without a valid rotation toward the target at hit time.
  - Conservative thresholds; enabled by default; profile/leniency aware.
- **Aim** (new, `checks/impl/aim/`, `RotationCheck`):
  - `AimAcceleration` — implausible rotation-acceleration spikes.
  - `AimSensitivity` — GCD / sensitivity consistency.
  - Start **experimental / observe-only** (high setback / no punish wiring) to bank
    false-positive data before tightening.
- **Reach** (strengthen existing `checks/impl/combat/Reach.java`): hitbox-expansion
  and lag-compensation tuning. No new file.

**Config.** New `checks.enabled.*` defaults + per-check threshold keys; descriptions
in `checks/en.yml`. New combat checks default enabled (except Aim subchecks:
disabled/observe-only).

**Files.** New check classes under `combat/` and `aim/`; `CheckManager` builder
entries (packetChecks for KillAura, rotationChecks for Aim); `Reach.java` edits;
`checks/en.yml`; config spec.

**Tests.** Per-check unit tests driven by synthetic packet/rotation sequences;
false-positive guards (teamed entities, active leniency windows, Bedrock skip).

---

## Phasing

Strict order; each phase builds green with its tests before the next:

0. Version-fix (standalone).
1. Alert pipeline + `AlertFeed` (Areas 3 and 4 depend on it).
2. Commands trim.
3. GUI rebuild (uses `AlertFeed` + profiles).
4. New combat checks.

## Risks & mitigations

- **False positives** (Area 4): ship conservative, Aim observe-only, all
  profile/leniency aware and tunable; bank data before tightening.
- **GUI rewrite surface** (Area 3): parity checklist vs. current `SacGUI` before
  deletion; holder routing removes the fragile title matching.
- **Config migration** (Areas 1–2): all new keys added via the existing
  `ConfigUpdater`; no keys removed; legacy behaviour reachable via flags.
- **Proxy alerts**: `[proxy]` route is unchanged; on a single-server minigame the
  removed `AutoPunishment.alertStaff` was the dominant double source.

## Out of scope

- New movement / scaffold / packet check families (combat only this round).
- Velocity/Bungee GUI (Bukkit-only).
- Removing any check or command class (disable-not-delete throughout).
