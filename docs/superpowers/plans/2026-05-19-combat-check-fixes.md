# combat Check Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix every `[BUG]`/`[FP]`/`[CONFIG]`/`[STYLE]` finding the audit recorded for the 14 `combat/` checks.

**Architecture:** Three implementation batches plus a verification task. Each check: fix its audit findings, wire `onReload(ConfigManager)` config, convert wall-clock timing to tick-based where flagged, build green, commit. Config defaults equal current hardcoded values, so no behaviour regression.

**Tech Stack:** Java, Minecraft anticheat (grim API fork). Build: `./gradlew build -x test`. No unit suite — build is the gate.

---

## Conventions (apply in every task)

**Findings source:** `docs/audit/2026-05-17-check-audit.md` — rows L127–L142 of the "All checks — audited" table, one row per combat check with every finding and line ref. Each step below names the fix; cross-reference the row for the exact line.

**Design source:** `docs/superpowers/specs/2026-05-19-combat-check-fixes-design.md`.

**Check directory:** `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/combat/`.

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

Use `getDoubleElse`/`getIntElse`/`getBooleanElse`. Key = `getConfigName() + ".<kebab-key>"`. Default arg = the existing hardcoded value. Fixed protocol constants become `private static final`. Match the `onReload` idiom in an already-fixed `crossapi/` or `movement/` check.

**no-reward fix.** Every clean (non-flagging) tick calls `reward()` exactly once. Where `@CheckData` has no `decay` set, add a sensible `decay` so `reward()` actually lowers VL.

**stale-state fix.** Counters/timers/flags reset on the path that ends the combat session or condition (death, teleport, item-switch, no-attack tick) — not only on the flagging path.

**wall-clock → tick timing.** Replace `System.currentTimeMillis()` detection windows with Grim's tick counter / transaction timing (the source sibling checks use). Where a millisecond window is genuinely needed, measure from a monotonic source. Read a sibling check that already does tick-based timing before converting.

**Build verification.** `./gradlew build -x test` — Expected: `BUILD SUCCESSFUL`.

---

## Task 1: Batch A — AimSnap … AutoArmor

**Files (Modify):**
- `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/combat/AimSnap.java`
- `.../combat/AimSuspicion.java`
- `.../combat/AntiVelocity.java`
- `.../combat/AttackFrequency.java`
- `.../combat/AutoArmor.java`

- [ ] **Step 1: AimSnap** — read audit row L127. Reset `buffer` on the `hadAttack=false` path (currently never resets → VL/buffer drift). Fix the inverted `snapYaw` semantics: it must compare the previous tick's yaw vs the attack-tick yaw in the correct order (currently inverted at L81). Add a `lastPacketWasTeleport` guard on the snap-back path. Replace the raw `int buffer` parallel to `violations` with the base-class buffer pattern if practical, else keep but ensure it decays. Wire config: `snap-threshold`, `return-threshold`, `diff-threshold`, `max-snap-back-packets`, `buffer-threshold` (the `>3`).
- [ ] **Step 2: AimSuspicion** — read audit row L128. Fix `hadRotationThisTick` so it measures rotation at the tick boundary, not within the same packet (L55-81). Fix `rotOnAttackOnly` so non-attack-tick rotation is also tracked — the ratio test is currently invalid because the denominator never sees non-attack rotation. Add teleport/lag/vehicle exemption so a legit always-rotate-on-click player does not hit the 95% ratio. Resolve the `[STYLE]`: the check is disabled-by-default but declares `setback=8` — set a consistent `@CheckData`. Wire config: `ratio-threshold` (0.95), `total-attack-ticks-min` (15), `buffer-threshold` (3).
- [ ] **Step 3: AntiVelocity** — read audit row L129. Size `ratioSamples` to the real window (the window yields up to 6 samples but the array is length 3 → silent overwrite) — set the array length to the actual maximum, or use a bounded ring buffer sized correctly. Move `sampleIndex` and sample mutation OFF the async lambda (L54) onto the check thread — no async mutation of detection state. Add boat/Levitation/Slow-Falling/Riptide/elytra exemption. Fix the `avgRatio<0.1` false flag on wall-pressing players (require an additional signal or exempt horizontal-collision). Wire config: `velocity-response-ticks`, `min-velocity`, `ground-friction`, `air-friction`, `avg-ratio-threshold` (0.1), `buffer-threshold`.
- [ ] **Step 4: AttackFrequency** — read audit row L130. Reset `rapidAttacks` when a combat session ends (no attack for N ticks) so it does not accumulate across sessions. Convert the 25 ms wall-clock check (L63, `System.currentTimeMillis()`) to tick-based timing. Add a teleport/lag exemption for the `attacksThisTick>1` path. Make the two flag paths' decay symmetric. Wire config: `min-attack-interval` (the 25 ms equivalent), `rapid-attacks-threshold` (5), `attacks-per-tick-threshold` (4), `buffer-threshold` (2).
- [ ] **Step 5: AutoArmor** — read audit row L131. Add an unconditional clean-tick `reward()` (currently VL is permanent). Set a `decay` on `@CheckData` so `reward()` is not a no-op. Trigger detection on an actual armor-slot switch, not `CLOSE_WINDOW` (L24). Add chest/GUI/creative/shift-click exemption. Wire config: `min-switch-delay` (50), `switch-count-threshold` (the `>10`), `fast-switch-threshold` (the `<3`).
- [ ] **Step 6: Build** — `./gradlew build -x test` → `BUILD SUCCESSFUL`.
- [ ] **Step 7: Commit**

```bash
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/combat/
git commit -m "fix(combat): batch A — audit fixes for AimSnap..AutoArmor"
```

---

## Task 2: Batch B — AutoClicker … MultiAttack

**Files (Modify):**
- `.../combat/AutoClicker.java`
- `.../combat/Criticals.java`
- `.../combat/FastBow.java`
- `.../combat/FastEat.java`
- `.../combat/MultiAttack.java`

- [ ] **Step 1: AutoClicker** — read audit row L132. Fix the `currentCPS` reset/increment edge case (L50-54) — ensure the counter rolls over cleanly at the window boundary. Align the cleanup window (5000 ms) with the CPS window (1000 ms) so variance is not computed from stale data (L56-87). Re-evaluate `MAX_LEGIT_CPS 25` — raise the default so legit jitter-clickers are not flagged. Set a `decay` on `@CheckData`. Wire config: `window-size` (20), `max-legit-cps` (25), `min-variance` (2), `cps-flag-threshold` (the `>18`), `cleanup-window-ms` (5000).
- [ ] **Step 2: Criticals** — read audit row L133. Update `lastDeltaY` every tick, not only on `INTERACT_ENTITY` (L41) — it is stale between attacks. Add onGround/teleport/water exemption. Fix the `deltaY>-0.01 && wasFalling` land-then-attack false flag (L31). Handle the 1.21.2+ `ATTACK` packet so the check is not blind on modern clients. Wire config: `flag-threshold` (the `>5`), `delta-y-threshold` (0.01).
- [ ] **Step 3: FastBow** — read audit row L134. Convert the `System.currentTimeMillis()` charge window (L48/L58) to tick-based timing. Reset `isDrawing`/`bowDrawStart` on item-switch, death, and teleport. Widen the 120 ms tolerance for players with >200 ms ping (add ping leniency, like the crossapi `CrossFastBow` fix — add tolerance, do not subtract). Restrict the `PLAYER_BLOCK_PLACEMENT` draw trigger to an actual bow item so an offhand-bow / generic right-click does not register a false draw (L39). Wire config: `min-charge-time` (120), `flag-threshold` (the `>3`).
- [ ] **Step 4: FastEat** — read audit row L135. Replace the fragile `isConsumable` `contains("_apple")` string match (double-matches golden apples) with a proper item-type check (use the same EDIBLE-attribute / explicit-type approach the crossapi `CrossFastEat` fix used). Reset `isUsing`/`useStartTime` on teleport, death, and item-switch. Exempt Creative instant-eat. Wire config: `min-eat-time` (1400), `min-drink-time` (1400 — currently unused; wire it and use it for drinkables), `flag-threshold` (the `>3`), `min-flag` (the `<1`).
- [ ] **Step 5: MultiAttack** — read audit row L137. Add a code comment documenting that the entity de-dup at L63 is intentional (A→B→A in one tick counts 2, the correct signal) — the audit notes it is correct but undocumented. Add a teleport exemption so `attacksThisTick` does not leak into the next tick. Resolve the `[STYLE]`: replace the inline-qualified `WrapperPlayClientPlayerFlying.isFlying` (L34) with a normal import.
- [ ] **Step 6: Build** — `./gradlew build -x test` → `BUILD SUCCESSFUL`.
- [ ] **Step 7: Commit**

```bash
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/combat/
git commit -m "fix(combat): batch B — audit fixes for AutoClicker..MultiAttack"
```

---

## Task 3: Batch C — MultiInteractB, NoSwingAttack, Reach, SelfInteract

**Files (Modify):**
- `.../combat/MultiInteractB.java`
- `.../combat/NoSwingAttack.java`
- `.../combat/Reach.java`
- `.../combat/SelfInteract.java`

- [ ] **Step 1: MultiInteractB** — read audit row L139. Fix the `hasInteracted` reset (L59) — it can clear the same invocation it was set; defer the reset one tick (same shape as the movement `InventoryMove` set+check fix). Replace the magic `3` in `isTickingReliablyFor(3)` (L68) with a shared named constant. Add a clean-path `reward()` (currently no `reward()` path).
- [ ] **Step 2: NoSwingAttack** — read audit row L140. Stop clearing `sentSwing` on every flying packet (L62) — make swing tracking ordering-robust (clear it on the tick boundary or after it is consumed, not unconditionally). Convert the 50 ms wall-clock window (L48) to tick-based timing. Fix the fragile spectator-exemption ordering (L44) — check the exemption before any state mutation. Add a 1.8 swing-reorder exemption. Wire config: `swing-window-ms` (the `>50` — converted), `flag-threshold` (the `>3`), `min-flag` (the `<2`).
- [ ] **Step 3: Reach** — read audit row L141. Add `reward()` on the clean-hit path in both `tickBetterReachCheckWithAngle()` and the main reach path (currently only `cancelBuffer` decays, `reward()` is never called — L306). Raise the `threshold` default `0.0005` (L356) to a value that does not false-positive on sub-pixel interpolation — pick a defensible value based on the check's expansion math and document it. Make `ATTACK_RANGE_COMPONENT_EXISTS` and `USE_1_8_HITBOX_MARGIN` resolve per-connection (per player's protocol version) instead of `static final`, so they are not stale on a runtime version change. Wire config: `threshold` (0.0005 → the raised default). Note the `Hitboxes` cross-check coupling at L241 — leave the coupling, it is intentional.
- [ ] **Step 4: SelfInteract** — read audit row L142. Resolve the TODO at L37: check the camera-entity ID so interacting with one's own camera entity is handled. Replace the unusual `flagAndAlert() && shouldModifyPackets()` chained idiom (L40) with a clear separate-statement form. Add a clean-path `reward()` and set a `decay` on `@CheckData` so VL does not accumulate forever.
- [ ] **Step 5: Build** — `./gradlew build -x test` → `BUILD SUCCESSFUL`.
- [ ] **Step 6: Commit**

```bash
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/combat/
git commit -m "fix(combat): batch C — audit fixes for MultiInteractB..SelfInteract"
```

> **Note for reviewers:** the wall-clock→tick timing conversions (AttackFrequency, FastBow, NoSwingAttack) and the `Reach` threshold raise change detection behaviour. Flag for in-game verification before this branch is shipped.

---

## Task 4: Config docs + final verification

**Files (Modify):**
- `common/src/main/resources/config/en.yml`
- `docs/audit/2026-05-17-check-audit.md`

- [ ] **Step 1: Document config keys** — collect every config key added in Tasks 1–3: `grep -rn "getDoubleElse\|getIntElse\|getBooleanElse" common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/combat/`. For each check, find its config name from its `@CheckData(... configName = "...")` annotation. Add a YAML block named after each config name to `common/src/main/resources/config/en.yml`, with every key, its default value, and a one-line comment — match the existing `crossapi`/`movement` config blocks' style and indentation exactly. Place new blocks after the last existing block. Validate: `python3 -c "import yaml; yaml.safe_load(open('common/src/main/resources/config/en.yml')); print('YAML OK')"`.
- [ ] **Step 2: Full build** — `./gradlew build -x test` → `BUILD SUCCESSFUL`.
- [ ] **Step 3: Confirm coverage** — find the base with `git merge-base main HEAD` (or the plan commit), run `git diff <base>..HEAD --stat`. Every one of the 14 combat checks in Tasks 1–3 must appear in the diff. `Hitboxes.java` and `MultiInteractA.java` must NOT appear (out of scope).
- [ ] **Step 4: Update audit report** — in `docs/audit/2026-05-17-check-audit.md`, set the `combat` row's `Pending` column (per-directory summary table) to `fixed — see plan 2026-05-19-combat-check-fixes`, and add a blockquote note above the first combat row in the "All checks — audited" table (match the style used for the crossapi/movement sections).
- [ ] **Step 5: Commit**

```bash
git add common/src/main/resources/config/en.yml docs/audit/2026-05-17-check-audit.md
git commit -m "docs: document combat config keys, mark combat audit findings resolved"
```
