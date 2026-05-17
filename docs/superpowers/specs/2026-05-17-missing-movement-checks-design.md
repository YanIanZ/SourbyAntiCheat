# Missing Movement Cross-API Checks

**Date:** 2026-05-17
**Status:** approved

## Architecture

All checks use existing `CrossValidationData` + prediction engine pattern.
No changes to original Grim checks. Cross-API checks in `crossapi/` serve as primary detection layer.

## 7 New Checks

### 1. FastFall
- **Type:** PostPredictionCheck
- **Primary:** `pePositionDeltaY < -3.5` (faster than terminal velocity ~3.92 blocks/sec × 0.05 = 0.196/tick)
- **Actual primary:** compare actual Y-delta vs predicted Y-delta. If actual fall exceeds predicted by > 0.5
- **Exemptions:** vehicle, gliding, fly, creative
- **Netty confirm:** `nettyPacketRatePerSec > 15`
- **Spartan confirm:** `NoFall` VL (fast fall often bundled with nofall)
- **Buffer:** +1.5 / +0.5, flag at > 3.0
- **Key:** `cross.fastfall`

### 2. CrossJump
- **Type:** PostPredictionCheck
- **Primary:** `pePositionDeltaY > 1.25` AND not on ground AND not jumping normally (clientVelocity.y <= 0.6)
- **Exemptions:** Jump Boost potion, Levitation, vehicle, fly, creative
- **Netty confirm:** `nettyAvgDelayBetweenPacketsMs < 50`
- **Spartan confirm:** `Step` VL (jump hacks similar to step)
- **Buffer:** +1.5 / +0.5, flag at > 3.0
- **Key:** `cross.jump`

### 3. CrossTeleport
- **Type:** PostPredictionCheck
- **Primary:** Per-tick distance > 8 blocks AND no teleport packet (`!lastPacketWasTeleport`)
- **Exemptions:** vehicle, ender pearl (tracked via teleport packets), creative, spectator
- **Netty confirm:** `nettyPacketRatePerSec < 10` (abnormal low rate during teleport-like movement)
- **Spartan confirm:** `Phase` VL
- **Buffer:** +2 / +1, flag at > 2 (immediate flag on strong signal)
- **Key:** `cross.teleport`

### 4. CrossFastLadder
- **Type:** PacketCheck
- **Primary:** Y-delta > 0.2 AND `Collisions.hasMaterial(player, boundingBox, LADDER/VINE)`
- **Exemptions:** fly, gliding, vehicle, creative
- **Netty confirm:** `nettyPacketRatePerSec > 18`
- **Spartan confirm:** `FastLadder` VL
- **Buffer:** +2 / +1, flag at > 3
- **Key:** `cross.fastladder`

### 5. DerpHead
- **Type:** RotationCheck
- **Primary:** Pitch outside [-80, 80] for > 20 consecutive ticks (head stuck in unnatural position)
- **Exemptions:** vehicle, spectator, dead, cinematic
- **Netty confirm:** `nettyIntervalVariance < 10` (consistent timing = automation)
- **Spartan confirm:** `IrregularMovements` VL
- **Buffer:** +2 / +1, flag at > 4
- **Key:** `cross.derp`

### 6. CrossFastEat
- **Type:** PostPredictionCheck
- **Primary:** USE_ITEM active duration < 1400ms (vanilla eating = 32 ticks = 1600ms)
- **Track:** onPacketReceive tracks USE_ITEM start time. onPredictionComplete checks if elapsed < threshold when item consumption ends
- **Exemptions:** creative, spectator
- **Netty confirm:** `nettyPacketRatePerSec > 18`
- **Spartan confirm:** `FastEat` VL
- **Buffer:** +1.5 / +0.5, flag at > 3.0
- **Key:** `cross.fasteat`

### 7. CrossFoodSprint
- **Type:** PostPredictionCheck
- **Primary:** Speed > 0.28 + `isUsingItem` state active + player is sprinting
- **Exemptions:** vehicle, fly, gliding, creative
- **Note:** Uses same `isUsingItem` tracker as CrossFastEat (shared via onPacketReceive)
- **Netty confirm:** `nettyPacketRatePerSec > 15`
- **Spartan confirm:** `NoSlowdown` VL
- **Buffer:** +1.5 / +0.5, flag at > 3.0
- **Key:** `cross.foodsprint`

## Implementation Tasks

1. CrossFastLadder: PacketCheck with LADDER/VINE material check
2. FastFall: PostPredictionCheck with Y-delta vs predicted comparison
3. CrossJump: PostPredictionCheck with Y-delta spike + potion exemption
4. CrossTeleport: PostPredictionCheck with distance > 8 + no teleport
5. DerpHead: RotationCheck with pitch anomaly tracking
6. CrossFastEat: PostPredictionCheck with USE_ITEM timing
7. CrossFoodSprint: PostPredictionCheck with speed + eating state
8. Register all 7 in CheckManager (appropriate type maps)
9. Full build verification
