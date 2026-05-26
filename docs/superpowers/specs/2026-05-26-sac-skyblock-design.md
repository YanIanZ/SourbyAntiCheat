# SAC Skyblock Integration — Design

Date: 2026-05-26
Branch: `feat/minigame-rewrite`
Status: Approved (design), pending implementation plan

## Goal

When a skyblock plugin is installed, automatically map its island world(s) to
the `SKYBLOCK` anticheat profile so island play (fly, generators, leniencies)
doesn't false-positive — with no manual config required.

Sub-project **#2 of 4** from the 2026-05-26 integration request
(after #1 hook framework; before #3 OldCombatMechanics, #4 version audit).

## Background (already handled — NOT in scope)

- **Island fly**: `FlightA` already exempts `player.canFly || player.isFlying`
  (set from the server→client abilities packet, which `setAllowFlight(true)`
  triggers). Prediction handles it too. No fly hook needed.
- **World→profile**: `ProfileWorldMap` glob-matches world names to profiles; the
  `SKYBLOCK` profile (`profiles.yml`) already disables FlightA/Tower/NoSlow + has
  leniencies. The only gap: the default glob (`island-*`) doesn't match real
  plugin world names.
- **TP to island**: teleports already exempted.

## What this adds

1. **`ProfileWorldMap.addMapping(String glob, Profile)`** — thread-safe runtime
   registration. `ProfileWorldMap` is currently immutable (built once from
   `profile-worlds.yml`); add a synchronized add + make `lookup` iterate a
   thread-safe view, so a hook can contribute mappings at enable time.

2. **`SkyblockWorldDetector`** (bukkit) — at enable, for each known skyblock
   plugin present (`Bukkit.getPluginManager().getPlugin(name) != null`), registers
   that plugin's default island world pattern(s) → `Profile.SKYBLOCK` via
   `addMapping`. No compileOnly plugin API — hardcoded known default patterns,
   so it is robust and dependency-free:
   - **SuperiorSkyblock2** → `SuperiorWorld*`
   - **BentoBox** → `bskyblock_world*`, `aoneblock_world*`, `acidisland_world*`,
     `caveblock_world*`
   - (IridiumSkyblock and custom world names: documented to use
     `profile-worlds.yml` — its default world name is too variable to hardcode.)

3. **Wiring** — call the detector in `SacBukkitLoaderPlugin.onEnable` immediately
   after `new ProfileWorldMap(...)` (line ~165), using that local reference,
   before the `ProfileResolver`/`ProfileRegistry` are built. Per-player `lookup`
   happens later, so mappings are present in time.

4. **Docs** — a comment block in `profile-worlds.yml` explaining that skyblock
   worlds are auto-mapped when the plugin is present, and how to override / add
   custom world names manually.

## Components & boundaries

- `ProfileWorldMap` (common) — owns glob→profile matching; gains `addMapping`.
- `SkyblockWorldDetector` (bukkit) — owns "which plugin → which world globs";
  pure mapping data + a Bukkit presence check; calls `addMapping`. No anticheat
  logic, no exemption state (that's the leniency/exemption systems' job).

## Error handling

- Detector wrapped so a failure can't abort enable (log + continue), matching
  the hook framework.
- Unknown/absent plugins simply skipped.

## Testing

- `ProfileWorldMapAddMappingTest` (pure): a mapping added via `addMapping` is
  returned by `lookup`; precedence (first-match) preserved with file mappings;
  non-matching world falls back. 
- `SkyblockWorldDetector` plugin-presence path is runtime-verified (Bukkit), but
  its plugin→globs table is exposed as a pure static map and unit-tested
  (each known plugin yields its expected globs).

## Out of scope

- IridiumSkyblock / custom world names (use `profile-worlds.yml`).
- Reading plugin APIs/configs for custom world names (dependency + fragility;
  not worth it given config covers it).
- Any new exemption logic — island fly is already handled.
