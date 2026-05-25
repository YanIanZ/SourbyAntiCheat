# Changelog

## [2.0.0] — 2026-05-25

Refocus of SourbyAntiCheat on minigame networks (bedwars / skywars / skyblock /
practice). The detection surface is narrowed to 37 reliable checks and a
per-arena **Profile** layer is introduced for threshold tuning and false-positive
leniency.

### Added
- `Profile` enum: `BEDWARS`, `SKYWARS`, `SKYBLOCK`, `PRACTICE`, `LOBBY`, `GENERIC`.
- Per-profile configuration in `profiles.yml`: per-check threshold overrides,
  disabled-check lists, and event-based leniencies.
- World → profile mapping in `profile-worlds.yml` (glob, first-match) with a
  configurable default profile.
- Hybrid profile resolution: API override → `sac.profile.<name>` permission →
  world-glob map → default.
- Public API on `SacAbstractAPI`: `getProfile` / `setProfile` / `clearProfile`,
  `grantLeniency` / `grantLeniencyAll` / `revokeLeniency` / `hasLeniency`,
  `fireLeniency`.
- Built-in leniency event handlers: `ENDER_PEARL_LAND`, `FIREBALL_BOOST`,
  `MLG_WATER_LAND`, `KIT_POTION_APPLY`, `ROD_PULL`, `SNOWBALL_KB`,
  `ELYTRA_FIREWORK_BOOST` — each grants short, per-profile exemptions to the
  movement/combat checks most prone to false flags during those actions.
- `ProfileAwareConfigView` overlays per-profile thresholds and disable flags on
  top of the base config when a check reloads.
- `Check.flag()` short-circuits while an active leniency window covers that check.

### Changed
- **Default detection set reduced to 37 checks.** The remaining ~190 checks are
  now **disabled by default** across all 13 locale config files. They are not
  removed — re-enable any of them per-server or per-profile if you need them.
- Kept checks: Reach, NoSwingAttack, AutoClicker, FastBow, FastEat, MultiAttack,
  SelfInteract, FlightA, Speed, NoSlow, Step, Spider, FastBreak, NoSwingBreak,
  FarBreak, ScaffoldA, FabricatedPlace, FarPlace, BedFucker, NoClip, ExploitA,
  ExploitB, ExploitC, BadPacketsA/B/C, PacketOrderA, NettyFlood, PayloadCheck,
  Post, TimerA, Tower, CrashA/B/C/D, MultiActionsA.

### Notes
- No check source was deleted, so plugins integrating against existing check
  classes continue to compile. Detection behavior changes only via the default
  enable flags and profiles.
- The existing `checks.yml` config pipeline is unchanged; the profile layer is
  additive (`profiles.yml` / `profile-worlds.yml`).
- A pre-refocus snapshot is preserved at branch `archive/pre-minigame-prune` and
  tag `archive-pre-prune-v1.0.0`.
