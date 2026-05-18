# movement Check Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix every `[BUG]`/`[FP]`/`[CONFIG]`/`[STYLE]` finding the audit recorded for the 15 `movement/` checks.

**Architecture:** Three implementation batches plus a verification task. Each check: fix its audit findings, wire `onReload(ConfigManager)` config, build green, commit. Config defaults equal current hardcoded values, so no behaviour regression. `Speed`/`Spider` are re-architected onto Grim's prediction engine in Task 3.

**Tech Stack:** Java, Minecraft anticheat (grim API fork). Build: `./gradlew build -x test`. No unit suite — build is the gate.

---

## Conventions (apply in every task)

**Findings source:** `docs/audit/2026-05-17-check-audit.md` — rows L273–L287 of the "All checks — audited" table, one row per movement check with every finding and line ref. Each step below names the fix; cross-reference the row for the exact line.

**Design source:** `docs/superpowers/specs/2026-05-18-movement-check-fixes-design.md`.

**Check directory:** `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/movement/`.

**Config wiring pattern.** For each `[CONFIG]` tunable threshold, add to the check:

```java
import ac.grim.grimac.api.config.ConfigManager;

private double someThreshold = 0.28; // default = prior hardcoded value

@Override
public void onReload(ConfigManager config) {
    String base = getConfigName() + ".";
    this.someThreshold = config.getDoubleElse(base + "some-threshold", 0.28);
}
```

Use `getDoubleElse`/`getIntElse`/`getBooleanElse`. Key = `getConfigName() + ".<kebab-key>"`. Default arg = the existing hardcoded value. Fixed protocol/physics constants (eye-height, frictions) become `private static final` instead — no config key. Match the `onReload` idiom in an already-fixed `crossapi/` check (e.g. `CrossFoodSprint`, `CrossTower`).

**no-reward / sparse-reward fix.** Every clean (non-flagging) tick calls `reward()` exactly once, ungated by `buffer < n`. Where a check has both a manual buffer decay and `reward()`, keep one decay path only.

**stale-state fix.** Counters/flags must reset on the path that ends the suspicious condition (long gap, clean tick, legal interval), not only on the flagging path.

**Build verification.** `./gradlew build -x test` — Expected: `BUILD SUCCESSFUL`.

---

## Task 1: Batch A — Blink … InventoryWalk

**Files (Modify):**
- `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/movement/Blink.java`
- `.../movement/EntitySpeed.java`
- `.../movement/FastLadder.java`
- `.../movement/InventoryMove.java`
- `.../movement/InventoryWalk.java`

- [ ] **Step 1: Blink** — read audit row L273. Reset `blinkCount` to 0 when a non-blink (normal-gap) packet arrives, so it stops flagging every packet forever past 5. Move `reward()` out of the `blinkCount<2` gate — call it unconditionally on every clean tick. Add a server-lag/GC/packet-batching exemption (skip when the server itself lagged the tick). Wire to config: `gap-threshold-ms` (500), `blink-count-threshold` (5), `decrement` (1).
- [ ] **Step 2: EntitySpeed** — read audit row L274. Replace the single `MAX_HORSE_SPEED 0.50` ceiling with a per-entity-type lookup (pig/horse/strider/boat/minecart distinct config defaults, mirror `CrossEntitySpeed`'s per-type approach). Add a speed-effect / attribute-modifier exemption on the ridden entity. Add teleport-after-mount grace and soul-sand/slime/server-push exemption. Wire to config: per-type `max-speed-*` (0.50), `buffer-increment` (0.5), `buffer-decay` (0.01).
- [ ] **Step 3: FastLadder** — read audit row L275. Before accumulating, confirm the player is actually on/against a ladder or vine — do not fire on an upward Y-delta `0.20–0.50` alone (kills the jump-arc false flag). Add water-current / Levitation / scaffold-beneath exemption. Remove the double-decay: keep either the manual `ladderBuffer` decay or `reward()`, not both. Wire to config: `max-ladder-speed` (0.20), `buffer-increment` (0.3), `buffer-decay` (0.01).
- [ ] **Step 4: InventoryMove** — read audit row L276. Fix the same-invocation set+check: set `hasOpenContainer` and act on it in separate ticks (defer one tick after the server opens the inventory). Add an unconditional clean-tick `reward()` / buffer decay (currently none). Add a knockback/velocity exemption while the inventory is open. Fix the unit mismatch: the `0.005` threshold is compared to a squared distance while verbose prints `sqrt` — compare like-for-like; use the squared form consistently and set the threshold to the intended value (document the chosen value in a comment). Wire to config: `move-threshold`.
- [ ] **Step 5: InventoryWalk** — read audit row L277. Detect inventory-open by the actual open-screen state, not only `CLICK_WINDOW windowId==0` (a client that avoids slot-clicks currently never trips). Fix the `inventoryOpen` same-invocation set+use (defer one tick). Add a closing-with-momentum grace so `INVENTORY_TIMEOUT_MS` does not flag a player who just closed the inventory. Wire to config: `inventory-timeout-ms` (3000), `move-threshold` (0.001), `buffer-threshold` (6).
- [ ] **Step 6: Build** — `./gradlew build -x test` → `BUILD SUCCESSFUL`.
- [ ] **Step 7: Commit**

```bash
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/movement/
git commit -m "fix(movement): batch A — audit fixes for Blink..InventoryWalk"
```

---

## Task 2: Batch B — Jesus … SetbackBlocker

**Files (Modify):**
- `.../movement/Jesus.java`
- `.../movement/NoRotate.java`
- `.../movement/NoSlow.java`
- `.../movement/NoWeb.java`
- `.../movement/SafeWalk.java`
- `.../movement/SetbackBlocker.java`

- [ ] **Step 1: Jesus** — read audit row L278. Initialise the local `lastY` from `player.y` on the first packet (not `0`) so the first-packet `deltaY` is not the full world Y. Better: remove the local `lastY` entirely and use `player.lastY` (the `[STYLE]` finding — `lastY` duplicates `player.lastY`). Add boat / lily-pad / slime-bounce exemption. Widen the `frac 0.85–0.99` water-walk band so legit high-latency bobbing does not hit it. Wire to config: `frac-min` (0.85), `frac-max` (0.99), `offset-threshold` (0.005), `buffer-threshold` (8), `buffer-flag` (5).
- [ ] **Step 2: NoRotate** — read audit row L279. Raise/qualify the 30-tick threshold so a legit sustained sprint while staring at a fixed target (mining a wall) does not flag within 1.5s — require an additional signal (e.g. movement direction changes while rotation is frozen). Add a stairs/slab/ice auto-level exemption. Collapse the redundant nested gating of `movesWithoutRotation` + `buffer` into one counter. Wire to config: `min-speed` (0.3), `tick-threshold` (30), `buffer-threshold` (3).
- [ ] **Step 3: NoSlow** — read audit row L280. Fix the stale `flaggedLastTick`: reset it to `false` whenever `isSlowedByUsingItem` is false, so a stale `true` cannot combine with a later tick into a false flag. Raise the `offsetToFlag 0.001` default — it is too tight and false-positives on high latency; pick a more lenient default and document it. Document the externally-set `didSlotChangeLastTick` field (add a comment explaining who sets it and why it is public). Wire to config: `offset-to-flag` (the new chosen default).
- [ ] **Step 4: NoWeb** — read audit row L281. Before accumulating, confirm the player is actually inside a cobweb block — do not fire on `deltaH 0.08–0.5` alone (normal walking at 0.217 currently grows the buffer). Add Speed-potion / soul-speed / dolphin's-grace exemption. Wire to config: `max-web-speed` (0.08), `secondary-threshold` (0.15), `offset-threshold` (0.005), `buffer-decay` (0.01).
- [ ] **Step 5: SafeWalk** — read audit row L282. Check both the X and Z axes (currently only X — pure-Z SafeWalk is undetected); `lastDeltaZ` is already tracked, wire it into the detection. If `lastDeltaZ` ends up genuinely unused after the fix, remove it (dead-code `[STYLE]`); otherwise it is now live. Widen the 10-tick window and add a knockback / wall / mob-push / ledge exemption so legit sudden stops do not flag. Wire to config: `delta-threshold` (0.05), `offset-threshold` (0.001), `tick-window` (10), `buffer-threshold` (2).
- [ ] **Step 6: SetbackBlocker** — read audit row L283. Add a `@CheckData` annotation (with `name`/`configName`) so `checkName`/`configName` are not null — removes the NPE risk if `reload`/`alert` is ever called. Remove the redundant in-vehicle `!lastPacketWasTeleport` re-check at L38 (already guarded at L29). No config keys (helper, no thresholds).
- [ ] **Step 7: Build** — `./gradlew build -x test` → `BUILD SUCCESSFUL`.
- [ ] **Step 8: Commit**

```bash
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/movement/
git commit -m "fix(movement): batch B — audit fixes for Jesus..SetbackBlocker"
```

---

## Task 3: Batch C — Step, Tower + Speed/Spider prediction re-architecture

**Files (Modify):**
- `.../movement/Step.java`
- `.../movement/Tower.java`
- `.../movement/Speed.java`
- `.../movement/Spider.java`

**Reference:** before editing `Speed`/`Spider`, read `crossapi/CrossSpeed.java` (commit `0a43cb4` physics-based rework) for the prediction-based pattern — how it consumes prediction offsets in `onPredictionComplete`, accumulates a `double` buffer, flags, and rewards on the clean path.

- [ ] **Step 1: Step** — read audit row L286. Raise the trigger so a 2nd consecutive step does not flag (`stepFlags>1` currently flags after 2 ticks; 1.9+ step-assist legitimately produces consecutive steps) — require a sustained run or a higher count. Add slime / honey / riptide exemption. Re-evaluate `MAX_STEP 0.63` against 1.9+ slab step-assist — raise the default if borderline. Wire to config: `max-step` (0.63), `flag-threshold` (5.0), `step-flag-increment` (1).
- [ ] **Step 2: Tower** — read audit row L287. Fix the `buffer` decay: it is decremented only when `yDelta<-1.0`, so an alternating small-negative / large-positive pattern never decays — decay on every clean tick instead. Reset `consecutiveJumps` on a legal jump interval (currently only decremented 1/jump, still reaches `>4`). Raise `MIN_JUMP_INTERVAL 200` ms to the vanilla minimum (~250 ms) so legit fast-jumpers are not caught — align with the comment at L34. Wire to config: `jump-threshold` (0.35), `min-jump-interval-ms` (the new ~250 default), `consecutive-jumps-threshold` (4), `buffer-threshold` (2).
- [ ] **Step 3: Speed re-architecture** — read audit row L284. Move detection from `onPacketReceive` into `onPredictionComplete`, consuming Grim's prediction offsets (follow the `CrossSpeed` reference). Add ice / soul-sand / slime / knockback / riptide exemption. Account for the Speed-effect amplifier in the allowed speed. Raise `MAX_EFFECT_SPEED 0.45` so creative-flight / elytra wind-burst is not falsely flagged (or exempt those states). Wire to config: `base-speed` (0.217), `sprint-speed` (0.281), `max-effect-speed` (0.45), `buffer-decay` (0.01), `flag-threshold` (1.0). Keep behaviour defaults equal to prior constants where the logic still uses them.
- [ ] **Step 4: Spider re-architecture** — read audit row L285. Move detection from `onPacketReceive` into `onPredictionComplete` (follow the `CrossSpeed` reference). Fix the early `climbTicks` increment: `wasOnGround` is set false only at L53, so the first airborne tick increments `climbTicks` one tick early — set/read `wasOnGround` so the first airborne tick is not counted. Add ladder / vine / water-current / scaffold / honey exemption. Wire to config: `climb-offset` (0.1), `climb-ticks-threshold` (4), `buffer-increment` (1).
- [ ] **Step 5: Build** — `./gradlew build -x test` → `BUILD SUCCESSFUL`.
- [ ] **Step 6: Commit**

```bash
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/movement/
git commit -m "fix(movement): batch C — Step/Tower fixes, Speed/Spider prediction re-architecture"
```

> **Note for reviewers:** the re-architected `Speed`/`Spider` change detection timing (packet → prediction). They cannot be verified by the build alone — flag for in-game verification before this branch is shipped.

---

## Task 4: Config docs + final verification

**Files (Modify):**
- `common/src/main/resources/config/en.yml`
- `docs/audit/2026-05-17-check-audit.md`

- [ ] **Step 1: Document config keys** — collect every config key added in Tasks 1–3 (grep the movement checks: `grep -rn "getDoubleElse\|getIntElse\|getBooleanElse" common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/movement/`). For each check, add a YAML block named after its config name to `common/src/main/resources/config/en.yml`, with every key, its default value, and a one-line comment — match the existing `crossapi` config blocks' style and indentation exactly. Validate the YAML parses.
- [ ] **Step 2: Full build** — `./gradlew build -x test` → `BUILD SUCCESSFUL`.
- [ ] **Step 3: Confirm coverage** — `git diff <merge-base>..HEAD --stat` (find base with `git merge-base main HEAD`). Every movement check in Tasks 1–3 must appear in the diff. `PredictionRunner`/`VehiclePredictionRunner` must NOT appear (out of scope).
- [ ] **Step 4: Update audit report** — in `docs/audit/2026-05-17-check-audit.md`, mark the movement section resolved: set the `movement` row's `Pending` column to `fixed — see plan 2026-05-18-movement-check-fixes`, and add a blockquote note above the first movement row in the "All checks — audited" table (match the style used for the crossapi section).
- [ ] **Step 5: Commit**

```bash
git add common/src/main/resources/config/en.yml docs/audit/2026-05-17-check-audit.md
git commit -m "docs: document movement config keys, mark movement audit findings resolved"
```
