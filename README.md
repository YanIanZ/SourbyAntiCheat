<div align="center">

# SourbyAntiCheat

**A packet-based, prediction-driven anticheat for modern Minecraft servers.**

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Release](https://img.shields.io/github/v/release/YanIanZ/SourbyAntiCheat?include_prereleases&label=release)](https://github.com/YanIanZ/SourbyAntiCheat/releases)
[![JitPack](https://img.shields.io/jitpack/version/com.github.YanIanZ/SourbyAntiCheat?label=jitpack)](https://jitpack.io/#YanIanZ/SourbyAntiCheat)

</div>

---

## Overview

SourbyAntiCheat (SAC) is an open-source Minecraft anticheat built on a movement
**prediction engine** (a fork of the Grim anticheat API) and raw packet inspection
via PacketEvents. Rather than relying on static thresholds alone, SAC simulates each
player's physically possible movement every tick and flags deviations, backed by a
large suite of packet, combat, and exploit checks.

Bedrock players connecting through Geyser are exempt by default to avoid false
positives from the Bedrock movement model. SAC is tuned for **minigame networks** —
per-arena profiles, event-aware leniency, and soft-depend integrations keep
detection tight without false-flagging legitimate game mechanics.

## Features

- **Prediction-based movement detection** — per-tick simulation of legal movement,
  with latency compensation and per-player world replication.
- **200+ checks** spanning movement, combat, scaffold/place, timer, packet
  validation, exploit/crash and aim. Checks follow a **disable-not-delete** model:
  irrelevant ones are unregistered (zero runtime cost) but the code stays, so any
  can be re-enabled. Combat coverage includes Reach/Hitbox, AutoClicker, KillAura
  (angle + target-switch), AimSnap, AntiKB/Velocity, AutoTotem and more.
- **Per-arena profiles** — assign a profile (e.g. `BEDWARS`, `SKYWARS`, `SKYBLOCK`,
  `LOBBY`) per world via glob in `profile-worlds.yml`; each profile (`profiles.yml`)
  tunes thresholds, disables checks and declares event leniencies. Resolution:
  API override → permission → world map → default.
- **Leniency & exemption layer** — game events (ender-pearl land, MLG water, rod
  pull, fireball/firework boost, kit potions…) open short, configurable windows
  where the relevant checks stand down; plugin abilities exempt checks while active.
- **Plugin integration hooks (soft-depend)** — presence-gated, never crash when a
  plugin is absent: **mcMMO** (exempt FastBreak/AutoClicker during super-abilities),
  **SuperiorSkyblock2 / BentoBox** (auto-map island worlds → `SKYBLOCK` profile),
  **OldCombatMechanics** (relax AutoClicker's raw-CPS path under 1.8 combat, keep
  inhuman-consistency detection).
- **One alert pipeline** — `punishments.yml`-driven staff alerts with a per-check
  cooldown (no duplicate spam), optional Discord/proxy/log routes, and a recent-alert
  feed surfaced in the GUI.
- **In-game GUI control panel** — `/sac gui`: paginated players → per-check VL +
  toggle, editable per-arena profiles, live alerts feed, reports and the ban-wave
  queue.
- **Cross-anticheat correlation** — the `crossapi` checks can cross-confirm
  violations with [Spartan](https://www.spigotmc.org/resources/spartan.118226/) via
  SpartanAPI; alerts agreed by both are tagged accordingly.
- **Multi-platform** — a shared `common` core with thin platform modules for
  Bukkit/Spigot/Paper, BungeeCord and Velocity.
- **Fully configurable** — every check threshold is exposed in `config/en.yml`;
  defaults are tuned so no key is required to get sensible behaviour.
- **Localised messages** — alert and command messages ship in multiple languages
  under `messages/`.

## Supported platforms

| Platform | Module | Notes |
|----------|--------|-------|
| Bukkit / Spigot / Paper | `bukkit` | Primary platform — runs the full check suite. |
| BungeeCord | `bungee` | Proxy module — alert relay and cross-server coordination. |
| Velocity | `velocity` | Proxy module — alert relay and cross-server coordination. |

Checks run on the backend (Bukkit) server. The proxy modules handle alert
forwarding between servers on a network.

## Installation

1. Download the jar for your platform from the
   [Releases](https://github.com/YanIanZ/SourbyAntiCheat/releases) page:
   - `SourbyAntiCheat.jar` — Bukkit / Spigot / Paper
   - `SAC-Bungee.jar` — BungeeCord
   - `SAC-Velocity.jar` — Velocity
2. Place the jar in the `plugins/` folder of the corresponding server/proxy.
3. Restart the server. Configuration files are generated on first run.

**Requirements**

- Java **21** or newer.
- A reasonably modern Spigot/Paper server for the backend.
- If using Geyser, install Floodgate on the backend server so SAC can identify
  Bedrock players.
- If using ViaVersion, install it on the **backend server only** — not on the proxy.

## Commands & GUI

All commands are under `/sac`. The default surface is the minigame core; legacy
and diagnostic commands are hidden unless `commands.legacy-enabled: true`.

| Command | Purpose |
|---------|---------|
| `/sac gui` | Open the control panel (players, profiles, alerts, reports, wave) |
| `/sac alerts` · `/sac verbose` · `/sac brands` | Toggle staff notifications |
| `/sac info <player>` · `/sac profile <player>` · `/sac history <player>` | Player intel |
| `/sac list` · `/sac top` · `/sac status` | Overview / health |
| `/sac toggle <check> <player>` · `/sac reset <player>` · `/sac exempt <player>` | Check management |
| `/sac reload` · `/sac version` · `/sac help` | Admin |

`/sac help` lists only the registered commands. The GUI routes by inventory
holder (not title text) and supports editing per-arena profiles in place.

## Configuration

Configuration lives in the plugin's data folder:

- `config/en.yml` — global settings and every check's tunable thresholds. Each
  check has a block named after its config name (e.g. `crossphase:`, `reach:`),
  with one commented key per threshold. Also holds `alerts.cooldown-ms`,
  `commands.legacy-enabled`, the `hooks:` block (mcMMO / OldCombatMechanics), and
  `punishment.legacy-auto-enabled`.
- `punishments.yml` — the single alert/punishment pipeline: groups of checks →
  `[alert]` / `[webhook]` / `[log]` / `[proxy]` / threshold commands.
- `profiles.yml` — per-arena profiles: disabled checks, threshold overrides and
  event leniencies.
- `profile-worlds.yml` — world-name glob → profile mapping (skyblock worlds are
  auto-mapped when a supported plugin is present).
- `messages/<lang>.yml` — alert and command message localisation.

Every threshold defaults to a sane built-in value, so keys are optional — add a
key only to override its default. Disable a check via `checks.enabled.<CheckName>`,
per arena in `profiles.yml`, or live in the GUI.

## Building from source

```bash
git clone https://github.com/YanIanZ/SourbyAntiCheat.git
cd SourbyAntiCheat
./gradlew build -x test
```

The build toolchain uses JDK 25; Gradle provisions it automatically. Output jars
land in each module's `build/libs/` folder:

- `bukkit/build/libs/SourbyAntiCheat.jar`
- `bungee/build/libs/SAC-Bungee.jar`
- `velocity/build/libs/SAC-Velocity.jar`

Pass `-Prelease=true` for a clean release version string.

## Using SAC as a dependency

The API is published via [JitPack](https://jitpack.io/#YanIanZ/SourbyAntiCheat).

**Gradle (Kotlin DSL)**

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.YanIanZ:SourbyAntiCheat:build-1")
}
```

**Maven**

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.YanIanZ</groupId>
    <artifactId>SourbyAntiCheat</artifactId>
    <version>build-1</version>
    <scope>provided</scope>
</dependency>
```

Replace the version with a release tag or commit hash.

## Contributing

Issues and pull requests are welcome. Keep changes focused, match the existing
code style, and ensure `./gradlew build -x test` passes before opening a PR.

## License

SourbyAntiCheat is licensed under the **GNU General Public License v3.0**.
See [LICENSE](LICENSE) for the full text.

Modified binaries, or plugins reusing SAC source, must remain private or make
their full source available to recipients at no additional cost, in accordance
with the GPLv3.
