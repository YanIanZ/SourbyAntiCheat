# SAC Movement/Combat Version-Coverage — Audit & Reference

Date: 2026-05-26
Branch: `feat/minigame-rewrite`
Status: Audit complete (sub-project #4 of 4). **No code changes** — reference only.

## Why this is a reference, not a code change

A static audit of movement/combat version handling found **no safe, concrete gap
to fix**. Version handling is centralized (prediction engine) and current
(through 1.21.11, packetevents-api 2.7.0). The remaining "deep hardening" would
mean editing a working prediction engine with high regression risk and no way to
verify without multi-version test clients — likely introducing bugs, not fixing
them. Version work should instead be **driven by concrete bug reports** (a
specific version + symptom), debugged precisely. This document is the triage map
for that.

## Where each layer handles versions

### Movement = the prediction engine (central), NOT per-check

Individual movement checks (`Speed`, `Step`, `Spider`, `Jesus`, `NoSlow`,
`FlightA`, `Tower`, `EntitySpeed`, …) contain **~0 version branches** — they
consume the engine's predicted offset and flag on divergence. All version-specific
physics live in the engine:

- `predictionengine/movementtick/MovementTicker.java` (~22 version refs)
- `predictionengine/PlayerBaseTick.java` (~22)
- `predictionengine/predictions/PredictionEngine.java` (~11) + `…Normal`/`…Water`/`…Elytra`
- `predictionengine/MovementCheckRunner.java` (~11)
- `predictionengine/PointThreeEstimator.java` (~7) — 1.9+ 0.03 movement threshold

**Implication:** to fix a movement version bug, change the engine, not the check.
Touching the engine requires a reproduction on the affected version.

### Combat = packet-timing / geometry (version-agnostic) + Reach

- `Reach.java` is the version-aware combat check (~11 refs): 1.7/1.8 hitbox
  margin, 1.20.5 `ENTITY_INTERACTION_RANGE` attribute, 1.21.11 `ATTACK_RANGE`
  item component, ViaVersion 1.8-hitbox handling.
- `Hitboxes`, `AutoClicker`, `AttackFrequency`, `MultiAttack`, `Criticals`,
  `AntiVelocity`, `AimSnap`, `AimSuspicion`, and the new `KillAuraA/B`/`AimAccel`
  are packet-timing or geometry based → behave the same across versions by
  construction (no version branch needed).

### Latency / cooldown utilities (combat-adjacent)

- `utils/latency/CompensatedCooldown.java` — 1.9+ attack cooldown.
- `utils/latency/CompensatedEntities.java`, `CompensatedInventory.java` — entity
  /inventory state across versions.

### Packet-tick boundaries (shared base)

- `checks/Check.java` + `MovementTicker.java` handle the 1.21.2 `CLIENT_TICK_END`
  packet (tick detection when no movement packet is sent that tick).

## Known version boundaries & where they live

| MC change | Version | Handled in |
|-----------|---------|-----------|
| 0.03 movement threshold | 1.9+ | `PointThreeEstimator`, `UncertaintyHandler` |
| Attack cooldown | 1.9+ | `CompensatedCooldown`, `Criticals` (fall-based) |
| 1.8 hitbox margin / reach | ≤1.8 | `Reach` (+ ViaVersion `use-1_8-hitbox-margin`) |
| Interaction-range attribute | 1.20.5+ | `Reach` (`Attributes.ENTITY_INTERACTION_RANGE`) |
| `CLIENT_TICK_END` packet | 1.21.2+ | `Check.isTickPacketIncludingNonMovement`, `MovementTicker` |
| `ATTACK_RANGE` item component | 1.21.11+ | `Reach` (`attackRangeComponentExists`) |

## Cross-version helpers already in place

- `player.getClientVersion()` / `PacketEvents.getServerManager().getVersion()` —
  used throughout for branching.
- ViaVersion/ViaBackwards aware: `utils/viaversion/ViaVersionUtil`
  (`isViaBackwardsPre1_9`, `use1_8HitboxMargin`) — `AttackFrequency` and `Reach`
  already adjust for Via-translated pre-1.9 clients.
- Bedrock: `Check.skipForBedrock` + `GeyserUtil.isBedrockPlayer` skip
  movement/combat checks for Geyser players.

## Triage guide — handling a version bug report

1. **Get the exact client + server version** and whether ViaVersion/ViaBackwards
   or Geyser is in play (these change packets/physics).
2. **Classify:** movement false/bypass → prediction engine; combat → Reach (if
   reach/hitbox) or the specific packet check; "only on version X" → look for a
   missing `isNewerThanOrEquals(V_X)` gate at the boundary in the table above.
3. **Reproduce on that version** (a test client or Via) before changing the
   engine — engine edits are unverifiable otherwise and regress easily.
4. **Prefer a version gate** (`if (clientVersion.isNewerThanOrEquals(...))`) over
   changing shared physics, to avoid regressing other versions.
5. Add a leniency/exemption (existing systems) if the divergence is a legitimate
   plugin/mechanic, rather than weakening the check globally.

## Conclusion

Movement/combat version handling is architecturally correct (centralized) and
current (1.8 → 1.21.11). No speculative changes made. Re-open with a concrete
version + symptom to action precisely.
