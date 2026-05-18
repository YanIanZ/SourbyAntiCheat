# Design — movement Check Fixes

**Date:** 2026-05-18
**Scope:** all 15 `movement/` checks with audit findings. Fix every `[BUG]`/`[FP]`/`[CONFIG]`/`[STYLE]` finding from `docs/audit/2026-05-17-check-audit.md` (rows L273–L287).

## Goal

Resolve every recorded audit finding for the `movement/` check directory, matching the treatment the `crossapi/` directory received (plan `2026-05-17-crossapi-check-fixes`).

## In scope

15 checks: `Blink`, `EntitySpeed`, `FastLadder`, `InventoryMove`, `InventoryWalk`, `Jesus`, `NoRotate`, `NoSlow`, `NoWeb`, `SafeWalk`, `SetbackBlocker`, `Speed`, `Spider`, `Step`, `Tower`.

## Out of scope

`PredictionRunner`, `VehiclePredictionRunner` — pure dispatcher helpers, no check logic, audited OK. Not touched.

## Architecture

**Config wiring.** Each check overrides `onReload(ConfigManager config)`, reading every `[CONFIG]` tunable via `getDoubleElse`/`getIntElse`/`getBooleanElse`. Key = `getConfigName() + ".<kebab-key>"`. Default arg = the current hardcoded value, so behaviour is unchanged until a server tunes config. Fixed protocol/physics constants become `private static final` instead — no config key. New keys are documented in `common/src/main/resources/config/en.yml` under a per-check block named after the check's config name (same convention as the existing `crossapi` blocks).

**Speed / Spider re-architecture.** Both checks currently have an empty `onPredictionComplete` and run detection off `onPacketReceive`, blind to Grim's prediction corrections. Detection moves into `onPredictionComplete`, consuming Grim's prediction offsets, using the recent `CrossSpeed` physics-based rework (commit `0a43cb4`) as the reference pattern. This is the highest-risk change; it is isolated into its own task and gets extra review.

**Build verification.** Project has no unit suite. `./gradlew build -x test` → `BUILD SUCCESSFUL` is the gate for every task. The re-architected `Speed`/`Spider` additionally need in-game verification — flagged as a note in the plan, not automatable here.

## Recurring bug-fix classes

- **no-reward / buffer-gated reward.** `Blink` rewards only when `blinkCount<2`; `FastLadder` has a manual decay *and* `reward()` (double-decay); `InventoryMove` never rewards. Fix: every clean (non-flagging) tick calls `reward()` exactly once, ungated; remove duplicate decay paths.
- **stale state never reset.** `Blink` `blinkCount` only increments; `NoSlow` `flaggedLastTick` persists when `isSlowedByUsingItem` is false; `Tower` `buffer` decremented only on `yDelta<-1.0`, `consecutiveJumps` never reset on a legal interval. Fix: reset/decay the state on the correct path.
- **detection without confirming preconditions.** `FastLadder` fires on any upward Y-delta without confirming the player is on a ladder; `NoWeb` fires on `deltaH` without cobweb verification; `Step` triggers on the 2nd consecutive step. Fix: verify the actual block/state before accumulating.
- **same-invocation set+check.** `InventoryMove`/`InventoryWalk` set `hasOpenContainer`/`inventoryOpen` and check it in the same invocation — the first tick after the server opens the inventory already flags. Fix: defer the check one tick.
- **wrong-unit comparison.** `InventoryMove` compares a `0.005` threshold against squared distance while verbose prints `sqrt` — effective threshold is ~0.07. Fix: compare like-for-like; pick the intended threshold explicitly.
- **axis coverage.** `SafeWalk` tracks `lastDeltaZ` but only checks the X axis — pure-Z SafeWalk is undetected. Fix: check both axes; remove the dead field if unused after the fix.
- **SetbackBlocker.** Missing `@CheckData` → null `checkName`/`configName` → NPE risk if `reload`/`alert` is called. Fix: add `@CheckData`. Remove the redundant in-vehicle `!lastPacketWasTeleport` re-check.

## FP exemptions (per finding)

Add the missing exemptions each audit row names: ice / soul-sand / slime / honey blocks, Levitation / Slow Falling, water-current, knockback / server-push velocity, riptide, lily-pad / boat, teleport-after-mount grace, cinematic-camera, stairs/slab auto-level, Speed-potion amplifier, soul-speed, dolphin's-grace. Each exemption is applied only to the check whose row names it.

## Task breakdown

- **Task 1 — Batch A:** `Blink`, `EntitySpeed`, `FastLadder`, `InventoryMove`, `InventoryWalk`.
- **Task 2 — Batch B:** `Jesus`, `NoRotate`, `NoSlow`, `NoWeb`, `SafeWalk`, `SetbackBlocker`.
- **Task 3 — Batch C:** `Step`, `Tower`, plus the `Speed` and `Spider` prediction-based re-architecture.
- **Task 4 — Config docs + verification:** document new `config/en.yml` keys; full build; confirm every movement check appears in the diff; mark the movement section resolved in the audit report.

Each implementation task: edit checks → wire config → `./gradlew build -x test` green → commit.

## Risks

- **Speed/Spider re-architecture** may shift detection behaviour. Mitigation: isolated task, extra review, in-game verification note, defaults preserved where possible.
- **FP exemptions** could over-broaden and create false negatives. Mitigation: apply only the exemptions the audit row names; do not add speculative ones.
- **Config defaults** must equal prior hardcoded values exactly — any mismatch is a behaviour regression. Mitigation: spec-review step verifies each default against the prior constant.
