# Design — combat Check Fixes

**Date:** 2026-05-19
**Scope:** all 14 `combat/` checks with audit findings. Fix every `[BUG]`/`[FP]`/`[CONFIG]`/`[STYLE]` finding from `docs/audit/2026-05-17-check-audit.md` (rows L127–L142).

## Goal

Resolve every recorded audit finding for the `combat/` check directory, matching the treatment `crossapi/` and `movement/` received.

## In scope

14 checks: `AimSnap`, `AimSuspicion`, `AntiVelocity`, `AttackFrequency`, `AutoArmor`, `AutoClicker`, `Criticals`, `FastBow`, `FastEat`, `MultiAttack`, `MultiInteractB`, `NoSwingAttack`, `Reach`, `SelfInteract`.

## Out of scope

`Hitboxes` (stub — flagged via `Reach` cross-check, no own logic) and `MultiInteractA` (audited OK — mirrors upstream Grim pattern). Not touched.

## Architecture

**Config wiring.** Each check overrides `onReload(ConfigManager config)`, reading every `[CONFIG]` tunable via `getDoubleElse`/`getIntElse`/`getBooleanElse`. Key = `getConfigName() + ".<kebab-key>"`. Default arg = the current hardcoded value, so behaviour is unchanged until tuned. Fixed protocol/physics constants become `private static final`. New keys are documented in `common/src/main/resources/config/en.yml` under a per-check block (same convention as the existing `crossapi`/`movement` blocks).

**Wall-clock → tick-based timing.** `AttackFrequency`, `FastBow`, `NoSwingAttack` (and any other check found using `System.currentTimeMillis()` for a detection window) currently measure time with wall-clock millis — vulnerable to GC pauses, OS scheduler jitter, and server lag. They are converted to Grim's tick/transaction-based timing (the engine's per-tick counter or transaction ping), the same source sibling checks already use. This is a behaviour shift: it is isolated to the timing tasks and gets extra review. Where a millisecond window is genuinely required, it stays millis but is measured from a monotonic source, not `currentTimeMillis()`.

**Build verification.** No unit suite. `./gradlew build -x test` → `BUILD SUCCESSFUL` is the gate for every task.

## Recurring bug-fix classes

- **no-reward / VL never decays.** `AutoArmor`, `Reach`, `SelfInteract` never call `reward()`, and some have `@CheckData` `decay` unset so `reward()` is a no-op. Fix: every clean tick calls `reward()`; set a sensible `decay` on the `@CheckData` where it is missing.
- **stale state across sessions.** `AttackFrequency` `rapidAttacks` persists across combat sessions; `AimSnap` `buffer` never resets on the no-attack path; `Criticals` `lastDeltaY` updated only on `INTERACT_ENTITY`; `FastBow`/`FastEat` never reset `isDrawing`/`isUsing` on item-switch/death/teleport. Fix: reset state on the path that ends the session/condition (death, teleport, item-switch, no-attack tick).
- **wall-clock timing** — see Architecture above.
- **concurrency.** `AntiVelocity` resets `sampleIndex` inside an async lambda (L54) and `ratioSamples` is length 3 but the window yields up to 6 → silent overwrite. Fix: size the buffer to the real window, and move index/sample mutation onto the check thread (no async mutation of detection state).
- **wrong-packet / wrong-semantics.** `AimSnap` `snapYaw` compares attack-tick yaw vs previous tick (inverted); `AutoArmor` fires on `CLOSE_WINDOW` not an armor switch; `Criticals` ignores the 1.21.2+ `ATTACK` packet (blind on modern clients); `FastBow` `PLAYER_BLOCK_PLACEMENT` triggers on any right-click (offhand-bow false draw); `FastEat` `isConsumable` string-matches `contains("_apple")` (double-matches golden apples). Fix: correct the packet/semantics per each finding.
- **same-invocation reset.** `MultiInteractB` `hasInteracted` reset can clear the same invocation it was set. Fix: defer the reset a tick (same pattern as the movement `InventoryMove` fix).

## FP exemptions (per finding)

Add the missing exemptions each audit row names: teleport / lag / vehicle exemptions, boat / Levitation / Slow Falling / Riptide / elytra (AntiVelocity), chest-GUI / creative / shift-click (AutoArmor), onGround / teleport / water (Criticals), high-ping tolerance widening (FastBow), creative instant-eat (FastEat), 1.8 swing-reorder (NoSwingAttack). Each exemption applies only to the check whose row names it.

## Reach specifics

`Reach` is the largest combat check. Fixes: add `reward()` on the clean-hit path (both `tickBetterReachCheckWithAngle()` and the main path); raise the `threshold` default `0.0005` to a value that does not false-positive on sub-pixel interpolation; make `ATTACK_RANGE_COMPONENT_EXISTS`/`USE_1_8_HITBOX_MARGIN` resolve per-connection rather than `static final` (stale on runtime version change). Config-wire `threshold`.

## Task breakdown

- **Task 1 — Batch A:** `AimSnap`, `AimSuspicion`, `AntiVelocity`, `AttackFrequency`, `AutoArmor`.
- **Task 2 — Batch B:** `AutoClicker`, `Criticals`, `FastBow`, `FastEat`, `MultiAttack`.
- **Task 3 — Batch C:** `MultiInteractB`, `NoSwingAttack`, `Reach`, `SelfInteract`.
- **Task 4 — Config docs + verification:** document new `config/en.yml` keys; full build; confirm every combat check appears in the diff; mark the combat section resolved in the audit report.

Each implementation task: edit checks → wire config → convert wall-clock timing → `./gradlew build -x test` green → commit.

## Risks

- **Wall-clock → tick conversion** shifts timing behaviour. Mitigation: isolated, extra review, in-game verification note for the affected checks.
- **AntiVelocity concurrency fix** must not introduce a different race. Mitigation: move all detection-state mutation to the check thread; reviewer verifies no async mutation remains.
- **Config defaults** must equal prior hardcoded values exactly. Mitigation: spec-review verifies each default against the prior constant.
- **Reach** is large and cross-coupled to `Hitboxes`. Mitigation: it is the sole non-trivial check in its batch; extra review.
