# SourbyAntiCheat Check Overhaul Design

Date: 2026-05-17
Status: Draft
Approach: A - Gradual Stabilize + Cross-API Layer

## Overview

This design covers four major changes to SourbyAntiCheat:
1. Remove the experimental check system entirely
2. Fix 17 problematic checks, disable 8 checks with fundamental flaws
3. Add 4 new cross-API detection checks (CrossSpeed, CrossAntiKB, CrossPhase, CrossTimer)
4. Strengthen SpartanAPI bundle to work standalone without external Spartan plugin

---

## Section 1: Remove Experimental System

### Changes

Remove the `experimental` concept entirely from the codebase:

1. **`CheckData.java`** — Remove `boolean experimental() default false` from the annotation
2. **`Check.java`** — Remove `@Getter private boolean experimental` field, remove `experimental` check from `flag()` method (line 119)
3. **`SacPlayer.java`** — Remove `experimentalChecks` field, getter, setter
4. **`ExperimentalChecksFeature.java`** — Delete entire file
5. **Config** — Remove `experimental-checks` config key from all language files
6. **All @CheckData annotations** — Remove `experimental = true` from all 72 check annotations
7. Checks previously marked experimental that are **DISABLE**d (see Section 2) get `enabled: false` in config defaults

### Disabled Check Defaults

The following checks will be set to `enabled: false` in config defaults (not deleted):
- AimSuspicion, AutoArmor, Blink, SafeWalk, BadPacketsAE, BadPacketsW, NettyDelay, NettyFlood

---

## Section 2: Fix Problematic Checks

### KEEP (16 checks - promote to non-experimental directly)
AttackFrequency, MultiAttack, BadPacketsM, BadPacketsP, BadPacketsV, BadPacketsC, PacketOrderA/D/F/G/H/J/K/L/M/N/O

### FIX (17 checks - bug fixes before promoting)

| Check | Fix Description |
|-------|----------------|
| AimSnap | Add vehicle/teleport exemptions; track multiple flying packets for snap-back instead of skipping on !hasRotationChanged |
| AntiVelocity | Add friction compensation to expected velocity; account for collision; reset on death/respawn/teleport; use multi-tick sampling |
| MultiInteractA | Extract sneaking state from ATTACK/SPECTATE packets instead of using stale lastSneaking |
| MultiInteractB | Replace Vector3d.equals() with epsilon-based comparison for floating-point positions |
| NoSwingAttack | Handle ATTACK packet type (PacketType.Play.Client.ATTACK) for 1.9+ clients |
| InventoryMove | Track server-initiated container close events (CLOSE_WINDOW from server) |
| NoRotate | Increase distance threshold to 0.3+; add elytra/gliding/flying exemptions; require recent combat context |
| Tower | Add exemptions for Jump Boost, Levitation, Slow Falling effects; verify block placement during jump |
| BadPacketsAC | Handle transaction ID wrapping for 1.17+ short-based IDs; detect forward-skipping IDs |
| BadPacketsAD | Exclude INTERACT_ENTITY ATTACK from "use" category; add 1.7 client exemption |
| BadPacketsH | Update lastSequence only after validating sequence is correct; don't corrupt on invalid sequences |
| BadPacketsR | Remove entity/piston cleanup side effects from the check; move to main tick loop |
| BadPacketsX | Reset sprint/sneak state on vehicle entry/exit |
| BadPacketsZ | Add flying-packet reset fallback for pre-1.21.2 clients without CLIENT_TICK_END |
| PacketOrderE | Fix duplicate "sprinting=" in verbose; add explicit parentheses for operator precedence |
| PacketOrderI | Replace `ac.grim.grimac.api.config.ConfigManager` import with project's own ConfigManager |
| PacketOrderP | Remove bundle-packet sending side effect from check class; move to packet handling code |

### DISABLE (8 checks - set enabled: false, needs redesign)
| Check | Reason |
|-------|--------|
| AimSuspicion | No non-attack control group; fundamental logic flaw |
| AutoArmor | 50ms threshold too aggressive; no slot validation |
| Blink | Network lag false positives; 500ms threshold too low |
| SafeWalk | Cannot distinguish from wall collision; X-axis only |
| BadPacketsAE | Flags all entity interactions; never validates entity existence |
| BadPacketsW | Empty stub; no implementation |
| NettyDelay | Per-packet timing unreliable for timer detection |
| NettyFlood | Circular dependency with checks that cancel packets |

### SacNettyChannelHandler Fixes
- Fix `detectProtocolState` method (broken protocol detection)
- Increase flood detection sensitivity (500 → 200 packets/second)
- Change `exceptionCaught` to only close on serious exceptions, not ClosedChannelException

---

## Section 3: Cross-API Detection Checks

### Architecture: CrossValidationEngine

New class `CrossValidationEngine` that aggregates data from all 4 API sources per-player:

```java
class CrossValidationData {
    // PacketEvents layer
    double pePositionDeltaX, pePositionDeltaY, pePositionDeltaZ;
    double peRotationDeltaYaw, peRotationDeltaPitch;
    long pePacketIntervalMs;
    int peFlyingPacketsPerTick;

    // Netty layer
    double nettyPacketRatePerSec;
    double nettyAvgReadBytesPerPacket;
    double nettyAvgDelayBetweenPacketsMs;

    // Spartan layer
    int spartanVL;
    Map<String, Integer> spartanPerCheckVL;
    double spartanAgreementRate;

    // SACAPI layer (internal prediction)
    double predictedDeltaX, predictedDeltaY, predictedDeltaZ;
    double offsetFromPrediction;
    double uncertaintyFactor;
}
```

Each SacPlayer gets a `crossValidationData` field updated by their respective packet/data handlers.

### New Checks

#### CrossSpeed (extends PostPredictionCheck)
- @CheckData(name = "CrossSpeed", configName = "crossspeed", decay = 0.05, setback = 25)
- Primary: SACAPI prediction engine offset
- Secondary: Netty packet timing anomaly
- Cross-validate: Spartan Speed VL agreement
- Flag condition: prediction offset > threshold AND (Spartan agrees OR Netty timing anomaly)

#### CrossAntiKB (extends PostPredictionCheck)
- @CheckData(name = "CrossAntiKB", configName = "crossantikb", decay = 0.15, setback = 15)
- Primary: SACAPI velocity response tracking (post-velocity movement ratio)
- Secondary: PacketEvents velocity packet verification
- Cross-validate: Spartan AntiVelocity VL
- Flag condition: movement ratio < threshold AND (Spartan flag OR 2 consecutive independent confirmations)

#### CrossPhase (extends PacketCheck)
- @CheckData(name = "CrossPhase", configName = "crossphase", decay = 0.05, setback = 10)
- Primary: PacketEvents position inside blocks
- Secondary: Netty packet stream gap detection
- Cross-validate: SACAPI Phase check data
- Flag condition: player inside block OR gap in packet stream > threshold, confirmed by Netty timing

#### CrossTimer (extends PacketCheck)
- @CheckData(name = "CrossTimer", configName = "crosstimer", decay = 0.01, setback = 50)
- Primary: PacketEvents balance method (existing Timer logic)
- Secondary: Netty per-packet timing analysis
- Cross-validate: Transaction-based timing (existing approach)
- Flag condition: balance deviation AND (Netty timing confirms OR transaction timing confirms)

### Registration
All 4 checks registered in CheckManager under appropriate check type maps (packetChecks or postPredictionChecks).

---

## Section 4: SpartanAPI Standalone Bundle

### API Enhancement (`me.vagdedes.spartan.api.API`)
- Add `isCheckEnabled(Player, HackType)` — check if a specific check is enabled
- Add `getPlayerData(Player)` — return structured violation data
- Add `getOnlinePlayers()` — return all monitored players
- Fix `getVersion()` to return correct SAC version

### SpartanCrossCheck Improvements
- Replace reflection-based `getBukkitPlayer()` with direct `PlatformPlayer.getPlayer()` call
- Add fallback: if direct call fails, use SAC internal violation data
- Add per-check VL caching (TTL: 5 seconds)
- Add `min-agreement-rate` config: only flag if Spartan agreement rate >= 0.6
- Config key: `spartanapi.cross-check.min-agreement-rate`

### SpartanEventBridge Improvements
- Replace reflection with `PlatformPlayer` API
- Add `SpartanViolationEvent` on SAC event bus (not just Bukkit) for non-Bukkit consumers
- Add deduplication: don't fire event within 500ms window for same player+check

### Enums.java Expansion
Add mappings for all SAC check names to HackType enum:
- Aim, Scaffold, Timer, Phase, Blink, NoSlow, Sprint, Elytra, InventoryMove, etc.

### Configuration
New config section:
```yaml
spartanapi:
  enabled: true
  bundle-mode: standalone
  cross-check:
    enabled: true
    min-vl: 3
    min-agreement-rate: 0.6
  event-bridge:
    enabled: true
    deduplication-window-ms: 500
```

### Build
No changes needed — SpartanAPI classes are already bundled in the jar.