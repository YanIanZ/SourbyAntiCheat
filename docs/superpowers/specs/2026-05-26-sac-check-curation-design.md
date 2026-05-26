# SAC Check Curation (remove elytra/vehicle + common subchecks) — Design

Date: 2026-05-26
Branch: `feat/minigame-rewrite`
Status: Approved (design), pending implementation plan

## Goal

Trim mechanic-irrelevant checks (no elytra/vehicles in the minigames) and add a
few common, alert-only movement/scaffold/fall subchecks for extra surface.

## Remove (unregister — keep class files, reversible)

From `CheckManager`:
- **Elytra**: `ElytraA`..`ElytraI` (9) — postPredictionChecks.
- **Vehicle**: `VehicleA`,`VehicleB`,`VehicleC`,`VehicleD`,`VehicleE`,`VehicleF` (6).

**Keep**: `VehiclePredictionRunner` (movement-prediction infra; dormant without
vehicles) and `VehicleTimer` (timer family). Crossapi elytra/vehicle checks
(Spartan-only, dormant) untouched.

No external references break (verified: elytra/vehicle checks aren't fetched via
`getCheck(X.class)` anywhere). Resource/config name references are inert.

## Add (3 new checks — alert-only, conservative, profile/leniency-aware)

All extend `Check`, `setback = -1` (alert-only; no auto-setback/punish), conservative
defaults + buffers, config-tunable. They overlap existing prediction-based checks
by design (operator wanted extra surface); kept alert-only to avoid punishment FP.

1. **`SpeedB`** (`movement/`, `PostPredictionCheck`) — blatant horizontal-speed cap.
   On `onPredictionComplete`: horizontal delta = `hypot(x-lastX, z-lastZ)`. Flag when
   `> max-horizontal` (default **0.9** blocks/tick; sprint-jump peaks ~0.6) for
   `> flag-threshold` (4) consecutive ticks. Exempt: vehicle, flying/canFly,
   gliding, swimming, and the tick after a teleport. (Subtle speed is left to the
   prediction `Speed` check; this only nets blatant 2×+ speed.)

2. **`ScaffoldC`** (`scaffolding/`, `BlockPlaceCheck`) — god-bridge signature.
   On `onBlockPlace`: block placed at/below feet (`position.y < player.y`), moving
   horizontally (`>0.15`/tick), looking down (`pitch > 40`), and **not sneaking** →
   `streak++`; flag at `> streak-threshold` (6). Exempt vehicle/flying. (Distinct
   from `ScaffoldA`'s interval/streak heuristic by the rotation+no-sneak signature.)

3. **`NoFallB`** (`groundspoof/`, `PacketCheck`) — sustained-descent ground claim.
   On a flying packet with `isOnGround() == true`, if the player is still descending
   (`y - lastY < -0.1`) for `> consecutive` (3) consecutive such packets → flag
   (real ground arrests descent within a tick; a single landing tick won't trip the
   buffer). Exempt vehicle/flying/gliding. (Simple packet heuristic alongside the
   prediction `GroundSpoof`.)

### Registration
- `SpeedB` → postPredictionChecks; `ScaffoldC` → blockPlaceChecks; `NoFallB` →
  packetChecks. `@CheckData` + per-check config defaults via `onReload`.

### Config
Per-check keys with defaults (read in `onReload`), e.g. `SpeedB.max-horizontal`,
`SpeedB.flag-threshold`, `ScaffoldC.streak-threshold`, `ScaffoldC.pitch`,
`NoFallB.consecutive`. No global config-version bump needed (checks fall back to
defaults; `checks.enabled.<name>` defaults true).

## Testing

- The pure geometry (horizontal-speed magnitude) is trivial; check logic follows
  the codebase's runtime-tested pattern (like ScaffoldA/NoFall — no unit tests).
- Build green + manual smoke (watch alert feed for FP; tune thresholds up).

## Risks

- All three overlap existing prediction checks and carry FP risk (operator
  accepted). Mitigation: alert-only (`setback=-1`), conservative defaults, buffers,
  exemptions, and full config tunability + per-profile disable + GUI toggle.

## Out of scope

- Timer subcheck (family already saturated: Timer/TickTimer/TimerLimit/NegativeTimer).
- Removing crash/exploit/packetorder/badpackets/crossapi (protective or Spartan-gated; kept).
- Deleting class files (unregister only).
