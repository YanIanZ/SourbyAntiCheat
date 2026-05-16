# Cross-API Checks Expansion

**Date:** 2026-05-17
**Status:** approved

## Architecture

All new checks use the existing `CrossValidationData` pattern — primary detection via PacketEvents, cross-validated with Netty timing data + Spartan Violation Levels. A check only flags when **primary signal (PE)** AND **at least one secondary source (Netty OR Spartan)** both confirm.

4 new fields added to `CrossValidationData`:
- `peOnGround` (boolean) — for NoFall ground-vs-air detection
- `peAttackIntervalMs` (double) — time between attack packets, for KillAura
- `peGliding` (boolean) — elytra gliding state
- `nettyIntervalVariance` (double) — spread of packet intervals for timing analysis

## 7 New Checks

### CrossFlight
- **Type:** PostPredictionCheck
- **Primary:** `offsetFromPrediction > 1.0 AND pePositionDeltaY >= 0` (not falling)
- **Netty confirm:** `nettyPacketRatePerSec > 25`
- **Spartan confirm:** Flight VL > 0
- **Buffer:** +1.5 if dual-confirmed, +0.5 if prediction-only; flag at > 3.0
- **Decay:** 0.05, **Setback:** 25, **Key:** `cross.flight`

### CrossJesus
- **Type:** PostPredictionCheck
- **Primary:** `abs(pePositionDeltaY) < 0.01` while `player.wasTouchingWater AND offsetFromPrediction > 0.8`
- **Netty confirm:** `nettyPacketRatePerSec > 22`
- **Spartan confirm:** Jesus VL > 0
- **Buffer:** +1.5 / +0.5; flag at > 3.0
- **Decay:** 0.05, **Setback:** 15, **Key:** `cross.jesus`

### CrossStep
- **Type:** PacketCheck
- **Primary:** `pePositionDeltaY > 0.6` in single tick AND `!player.jumping` (not a normal jump)
- **Netty confirm:** `nettyAvgDelayBetweenPacketsMs < 40 AND nettyPacketRatePerSec > 18`
- **Spartan confirm:** Step VL > 0
- **Buffer:** counter +2 / +1; flag at > 3
- **Decay:** 0.1, **Setback:** 10, **Key:** `cross.step`

### CrossSpider
- **Type:** PacketCheck
- **Primary:** `pePositionDeltaY > 0.15 AND pePositionDeltaY < 0.5 AND player.horizontalCollision`
- **Netty confirm:** `nettyPacketRatePerSec > 20`
- **Spartan confirm:** Spider VL > 0
- **Buffer:** counter +2 / +1; flag at > 4
- **Decay:** 0.1, **Setback:** 10, **Key:** `cross.spider`

### CrossNoFall
- **Type:** PostPredictionCheck
- **Primary:** `offsetFromPrediction Y-component > 0.3 AND peOnGround == true` but prediction says airborne > 3 ticks
- **Netty confirm:** `nettyAvgDelayBetweenPacketsMs < 50`
- **Spartan confirm:** NoFall VL > 0
- **Buffer:** +1.5 / +0.5; flag at > 4.0
- **Decay:** 0.15, **Setback:** 15, **Key:** `cross.nofall`

### CrossElytraMove
- **Type:** PacketCheck
- **Primary:** `peGliding == true AND velocity magnitude > 30 m/s`
- **Netty confirm:** `nettyPacketRatePerSec > 20`
- **Spartan confirm:** ElytraMove VL > 0
- **Buffer:** counter +2 / +1; flag at > 3
- **Decay:** 0.05, **Setback:** 12, **Key:** `cross.elytramove`

### CrossKillAura
- **Type:** PacketCheck
- **Primary:** `peRotationDeltaYaw > 30 degrees AND peAttackIntervalMs < 200 AND peAttackIntervalMs > 0`
- **Netty confirm:** `nettyPacketRatePerSec > 15 AND nettyIntervalVariance < 25`
- **Spartan confirm:** KillAura VL > 0
- **Buffer:** counter +2 / +1; flag at > 5
- **Decay:** 0.01, **Setback:** 50, **Key:** `cross.killaura`

## Implementation Tasks

1. Add 4 new fields to `CrossValidationData` + reset logic
2. Populate `peOnGround`, `peGliding`, `peAttackIntervalMs` in `CheckManagerListener`
3. Populate `nettyIntervalVariance` in `SacNettyChannelHandler`
4. Add `BoatMove` and `ElytraMove` Spartan HackType VL lookup support in `SpartanCrossCheck` (they already exist in Enums.HackType)
5. Create 7 cross-API check files in `checks/impl/crossapi/`
6. Register all 7 checks in `CheckManager`
7. Full build verification
