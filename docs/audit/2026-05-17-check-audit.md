# Check Audit — 2026-05-17

Read-only audit of anticheat checks. Tags: `[BUG]` `[FP]` `[CONFIG]` `[STYLE]`.

**Status:** 136 of 250 checks audited (15 of 21 directories). 6 directories
pending — `crossapi`, `movement`, `sprint`, `timer`, `vehicle`, `velocity`
(~92 checks) — subagent quota exhausted, resets 22:20 Asia/Jakarta. Re-run
audit agents for those dirs to complete.

## Top `[BUG]` findings (false-ban / crash risk)

- `aim/AimProcessor` — division by zero: `deltaXRot / modeX` with `modeX=0.0` default → `NaN`/`Infinity` propagates silently to dependent checks.
- `aim/AimAssist` — smooth-detect branch checks `deltaYaws.size() >= 40` but list capped at `SAMPLE_SIZE=30` → **unreachable dead code**.
- `baritone/Baritone` — comment says check disabled (cinematic-camera FPs) but no `isEnabled=false` / `DISABLED_BY_DEFAULT` entry → flags live players.
- `crash/CrashA` — `Math.abs(double) > Integer.MAX_VALUE` always false → Y-axis crash check is dead.
- `crash/CrashE` — modifies packet (`setViewDistance`) + flags with no `shouldModifyPackets()` guard → mutates packets for `sac.nomodifypacket` holders.
- `groundspoof/NoFall` — packet rewritten to `onGround=false` even when `flagAndAlertWithSetback` returns false → corrects ground claim for exempt players.
- `packetorder/PacketOrderG` — `canCancel(action)` called with `action == null` (CLIENT_STATUS path) → NPE in `onPacketReceive`.
- `misc/NettyFlood` — `player.cancelledPackets` (cumulative) never reset → permanent flag after first 10 cancelled packets.
- `badpackets/BadPacketsG` — `lastSneaking` never updated after flag → every subsequent sneak packet flags forever.
- `badpackets/BadPacketsAE` — `invalidCount` never decrements, no `reward()` → flags every INTERACT_ENTITY forever once past 20.
- `badpackets/BadPacketsD` — pitch clamp reads stale `player.pitch` field instead of local `pitch` → clamp is a no-op.
- `combat/AntiVelocity` — `ratioSamples` array length 3 but sample window yields up to 6 → silent overwrite/data loss; `sampleIndex` reset in async lambda corrupts window.
- `combat/AutoArmor` — fires on `CLOSE_WINDOW` (not armor switch) → false inter-event timing.
- `breaking/FastBreak` — `startBreak` 50ms grace inconsistently applied (only when `targetBlockPosition==null`).
- `crash/CrashI` — `e.getMessage().substring(27)` fragile parse → unchecked exception if packetevents reworas prefix.

**Pervasive:** most checks never call `reward()` → `violations` ratchets up
monotonically, decaying only via `@CheckData.decay` (often unset → 0.0).
This is the single most common finding across all directories.

## Per-directory summary

| Dir | Checks | Findings | OK | Pending |
|-----|--------|----------|-----|---------|
| aim | 4 | 4 | 0 | — |
| badpackets | 36 | 35 | 1 | — |
| baritone | 1 | 1 | 0 | — |
| breaking | 10 | 10 | 0 | — |
| chat | 4 | 4 | 0 | — |
| combat | 16 | 14 | 2 | — |
| crash | 9 | 9 | 0 | — |
| crossapi | 54 | — | — | PENDING |
| elytra | 9 | 9 | 0 | — |
| exploit | 3 | 3 | 0 | — |
| flight | 1 | 1 | 0 | — |
| groundspoof | 1 | 1 | 0 | — |
| misc | 10 | 9 | 1 | — |
| movement | 17 | — | — | PENDING |
| multiactions | 8 | 8 | 0 | — |
| packetorder | 17 | 17 | 0 | — |
| prediction | 4 | 4 | 0 | — |
| scaffolding | 11 | 11 | 0 | — |
| sprint | 7 | — | — | PENDING |
| timer | 5 | — | — | PENDING |
| vehicle | 6 | — | — | PENDING |
| velocity | 3 | — | — | PENDING |

## All checks — audited

| Dir | Check | Status | Findings |
|-----|-------|--------|----------|
| aim | AimAssist | findings | `[BUG]` no `reward()` while accumulating samples L51 · `[BUG]` smooth-detect `size()>=40` unreachable (cap 30) L70 dead code · `[BUG]` snap-streak else-path no `reward()` L63 · `[FP]` no ping/lag exemption (rubber-band yaw spikes) · `[FP]` no respawn/world-change rotation-reset guard · `[CONFIG]` `SAMPLE_SIZE 30`, `SNAP_THRESHOLD 25.0`, `SMOOTH_VARIANCE_MAX 0.15`, range `2.0/10.0`, streak `5` hardcoded · `[STYLE]` two detection paths share one VL with no sub-check label |
| aim | AimDuplicateLook | findings | `[BUG]` no `reward()` anywhere — VL monotonic · `[FP]` `exempt` covers only 1 tick post-teleport · `[FP]` no spectator/dead/world-change exemption · `[FP]` no lag exemption (packet reorder) · `[STYLE]` `@CheckData` missing decay/setback/description |
| aim | AimModulo360 | findings | `[FP]` L31 `yaw<360 && yaw>-360` true for all normal yaw — no discrimination · `[FP]` no lag exemption · `[STYLE]` `lastDeltaYaw` updated asymmetrically between branches |
| aim | AimProcessor | findings | `[BUG]` division by zero L72-73 `deltaXRot/modeX` with `modeX=0.0` default → NaN/Infinity propagates to consumers · `[STYLE]` extends `Check` but is a processor (no flag/reward) — architectural mismatch |
| badpackets | BadPacketsA | findings | `[BUG]` no `reward()` on non-duplicate path · `[FP]` `lastSlot` not reset on respawn/world-change |
| badpackets | BadPacketsB | findings | `[BUG]` no `reward()` anywhere |
| badpackets | BadPacketsC | findings | `[BUG]` no `reward()` · `[FP]` no exemption for bed-destroy vs LEAVE_BED race |
| badpackets | BadPacketsD | findings | `[BUG]` clamp L27-28 reads stale `player.pitch` not local `pitch` → no-op · `[BUG]` no `reward()` · `[FP]` no NaN/Infinity guard |
| badpackets | BadPacketsE | findings | `[BUG]` no `reward()` — fires every tick once over threshold · `[CONFIG]` `maxNoReminderTicks` 19/20 hardcoded L17 |
| badpackets | BadPacketsF | findings | `[BUG]` no `reward()` on valid sprint-state-change path |
| badpackets | BadPacketsG | findings | `[BUG]` `lastSneaking` never set after flag L26-30 → every subsequent START_SNEAKING flags; mirror bug STOP_SNEAKING L36-40 · `[BUG]` no `reward()` |
| badpackets | BadPacketsH | findings | `[BUG]` alert path not clearly gated by version guard L56-65 · `[FP]` sequence reset to 0 on world-change L68 · no `reward()` |
| badpackets | BadPacketsI | findings | `[BUG]` no `reward()` · `[FP]` no gliding/elytra ability-toggle exemption |
| badpackets | BadPacketsJ | findings | `[BUG]` no `reward()` · `[BUG]` `flagAndAlert` inside `for` loop L52 — multi-flag/tick no buffer |
| badpackets | BadPacketsK | findings | `[BUG]` no `reward()` · `[STYLE]` duplicates BadPacketsAH (K cancels, AH only flags) |
| badpackets | BadPacketsL | findings | `[BUG]` no `reward()` · `[CONFIG]` magic `255` pre-1.7 SOUTH face L33 |
| badpackets | BadPacketsM | findings | `[BUG]` no `reward()` · `[STYLE]` `flag()` without alert — staff never see violation |
| badpackets | BadPacketsN | findings | `[STYLE]` empty stub — no logic |
| badpackets | BadPacketsO | findings | `[BUG]` no `reward()` · `[FP]` keepalive list unbounded if client never responds |
| badpackets | BadPacketsP | findings | `[BUG]` no `reward()` · `[CONFIG]` magic button-bounds `2/8/40/10` L42-46 · `[FP]` TODO L40 container-type adjustment missing |
| badpackets | BadPacketsQ | findings | `[BUG]` no `reward()` · `[CONFIG]` magic `100` max jump boost L23 · `[BUG]` entity-ID mismatch flags regardless of action (`||` grouping L23-25) |
| badpackets | BadPacketsR | findings | `[BUG]` uses `flag()` not `flagAndAlert()` L28 — no staff alert · `[CONFIG]` magic `2000`/`5000` ms L26 |
| badpackets | BadPacketsS | findings | `[BUG]` no `reward()` |
| badpackets | BadPacketsT | findings | `[BUG]` no `reward()` · `[CONFIG]` magic `0.3001/1.8001/0.0001/0.1` L29-31 · `[FP]` no exemption for changed hitbox (sit/sneak) |
| badpackets | BadPacketsU | findings | `[BUG]` no `reward()` · `[CONFIG]` magic `4095`/`255` expected-Y L32 |
| badpackets | BadPacketsV | findings | `[BUG]` no `reward()` · `[BUG]` `flagAndAlert` fires every below-threshold packet, no once-per-window gate · `[FP]` no knockback/teleport micro-move exemption |
| badpackets | BadPacketsW | findings | `[STYLE]` disabled-by-default empty stub — dead code |
| badpackets | BadPacketsX | findings | `[BUG]` no `reward()` on `!canSkipTicks` path when `flags==0` L24-31 · `[STYLE]` early return skips `flagAndAlertWithSetback` |
| badpackets | BadPacketsY | findings | `[BUG]` no `reward()` |
| badpackets | BadPacketsZ | findings | `[BUG]` no `reward()` · `[FP]` 1.21.2 clients get double-reset; `sent` never clears if CLIENT_TICK_END lost |
| badpackets | BadPacketsAA | findings | `[BUG]` no `reward()` · `[STYLE]` pitch>90 overlaps D & AJ — inconsistent thresholds (`90`/`90.1f`/`90.0f`) |
| badpackets | BadPacketsAB | findings | `[BUG]` no `reward()` · `[FP]` steer values not range-validated `[-1,1]` |
| badpackets | BadPacketsAC | OK | — |
| badpackets | BadPacketsAD | findings | `[FP]` no exemption for animation-free USE_ITEM (eat/drink/bow/trident/shield) · `[CONFIG]` magic `3` L42 |
| badpackets | BadPacketsAE | findings | `[BUG]` `invalidCount` never decrements, no `reward()` → flags forever past 20 · `[BUG]` no entity-existence check · `[CONFIG]` magic `20` L25 |
| badpackets | BadPacketsAF | findings | `[BUG]` no `reward()` · `[FP]` threshold `0.05` magic; missing slow-fall/scaffold-descend/honey/step-up exemptions |
| badpackets | BadPacketsAG | findings | `[BUG]` no `reward()` · `[BUG]` slot-range `[-1,45]` check applies creative protocol to non-creative wrongly · `[FP]` packet not cancelled |
| badpackets | BadPacketsAH | findings | `[BUG]` no `reward()` · `[STYLE]` duplicates BadPacketsK — inconsistent enforcement |
| badpackets | BadPacketsAI | findings | `[BUG]` cancels packet L49 without `shouldModifyPackets()` · `[BUG]` no `reward()` · `[STYLE]` duplicates BadPacketsI; stale `serverAllowsFlight` |
| badpackets | BadPacketsAJ | findings | `[BUG]` NaN/Infinity cancel L45 without `shouldModifyPackets()` · `[STYLE]` pitch>90 triple-coverage D/AA/AJ · `[CONFIG]` magic `200/170/3` L65-67 · `[FP]` deltaPitch wrap not normalized |
| baritone | Baritone | findings | `[BUG]` `verbose` int not reset when outer pitch condition fails L31-37 — stale counts · `[BUG]` no `reward()` · `[BUG]` comment says disabled but no `isEnabled=false`/`DISABLED_BY_DEFAULT` → flags live players · `[FP]` no cinematic-camera exemption (named FP source) · `[FP]` no teleport/world-change exemption · `[CONFIG]` magic `8` L32, `1` L29, `90.0f` L29 · `[STYLE]` `verbose` int counter deviates from `violations`+`decay` pattern |
| breaking | AirLiquidBreak | findings | `[FP]` no `reward()` on clean break L64-68 · `[FP]` `SOUL_FIRE` not listed with `FIRE` L60 · `[BUG]` `didLastFlag` left true when `flagAndAlert` returns false L64-68 |
| breaking | FarBreak | findings | `[BUG]` no `reward()` clean path L40-43 · `[FP]` no teleport-lag exemption (stale position) L28-30 · `[FP]` DROP_ITEM/SWAP not filtered L22 |
| breaking | FastBreak | findings | `[CONFIG]` magic `275/300/1000/25/50` ms L70-105 · `[BUG]` `startBreak` 50ms grace inconsistently applied L70 · `[BUG]` no `reward()` — VL monotonic L77-113 · `[FP]` no lag-spike exemption beyond ping clamp |
| breaking | InvalidBreak | findings | `[BUG]` no `reward()` clean path L23-28 · `[BUG]` 1.7 face-255 exemption only on CANCELLED_DIGGING — START_DIGGING face-255 falsely flagged · `[STYLE]` no `MAX_FACE_ID` constant |
| breaking | MultiBreak | findings | `[BUG]` no `reward()` — VL unbounded L40-41 · `[BUG]` deferred flags have no cancel path L65-68 · `[FP]` `hasBroken` reset uses `||` — camera-entity transition suppresses accumulation L55 · `[STYLE]` `flags` cleared even when not ticking reliably — deferred flags dropped silently |
| breaking | NoSwingBreak | findings | `[BUG]` no `reward()` L35-36 · `[FP]` no creative/spectator exemption · `[FP]` no 1.8 swing-ordering exemption · `[FP]` no vehicle/riding exemption |
| breaking | PositionBreakA | findings | `[BUG]` no `reward()` clean path L57-59 · `[FP]` TRIPWIRE/RAIL/VINE not exempted alongside REDSTONE_WIRE L22 · `[FP]` eyePositions expansion conditional L36-38 — stale position under lag |
| breaking | PositionBreakB | findings | `[BUG]` no `reward()` L30-31 · `[BUG]` no buffer/threshold guard — every mismatched face is immediate flag L30 · `[FP]` `lastFace` persists across teleport · `[STYLE]` `255` vs `0` could be named constant |
| breaking | RotationBreak | findings | `[BUG]` `flagBuffer` hard-set to 1 on any miss L66 — lag-miss == repeat-offender · `[BUG]` 9/10 subsequent breaks pre-cancelled after single miss L41-64 · `[BUG]` no `reward()` on `violations` L64 · `[FP]` always-expand inconsistent with PositionBreakA L85 · `[STYLE]` `double` flagBuffer used as 10-step int counter |
| breaking | WrongBreak | findings | `[BUG]` no `reward()` L59-83 · `[BUG]` double-cancel state interaction — exemption fires unexpectedly for 2nd cancel L29-35 · `[FP]` `lastBlock`/`lastCancelledBlock` persist across teleport · `[STYLE]` `exemptedY` misleading name (holds -1 on 1.14+) |
| chat | ChatA | findings | `[BUG]` no `reward()` on clean packet L26 · `[BUG]` `flagAndAlert("")` empty verbose · `[FP]` no operator tab-complete exemption · `[CONFIG]` `V_1_13` hardcoded L23 |
| chat | ChatB | findings | `[BUG]` L29 unconditional `event.setCancelled(true)` for CHAT_MESSAGE bypasses `shouldModifyPackets()` · `[BUG]` no `reward()` on clean message · `[FP]` click-event FP acknowledged L15, no exemption · `[CONFIG]` `V_1_19` hardcoded L57 |
| chat | ChatC | findings | `[BUG]` no `reward()` · `[FP]` chat after landing/boat-ride can trigger — no vehicle/post-teleport exemption · `[FP]` no-op for pre-1.21.2 clients (no CLIENT_TICK_END) · `[STYLE]` malformed regex config → uncaught `PatternSyntaxException` at reload |
| chat | ChatD | findings | `[BUG]` `hidden` defaults false — join-with-chat-hidden window not caught · `[BUG]` no `reward()` · `[BUG]` Play-phase `WrapperPlayClientSettings` decodes Configuration-phase packet L33 — possible garbage values · `[FP]` legit chat-visibility toggle accumulates VL permanently · `[STYLE]` inconsistent Lombok usage |
| combat | AimSnap | findings | `[BUG]` `buffer` never resets on `hadAttack=false` path L93-96 — VL/buffer drift · `[BUG]` `snapYaw` compares attack-tick yaw vs previous tick — inverted semantics L81 · `[FP]` no `lastPacketWasTeleport` guard on snap-back path · `[CONFIG]` `SNAP/RETURN/DIFF_THRESHOLD`, `MAX_SNAP_BACK_PACKETS`, buffer `>3` hardcoded L31-89 · `[STYLE]` raw `int buffer` parallel to `violations` |
| combat | AimSuspicion | findings | `[BUG]` `hadRotationThisTick` measures same-packet not tick-boundary L55-81 · `[BUG]` `rotOnAttackOnly` never tracks non-attack-tick rotation → ratio test invalid L61-69 · `[FP]` no teleport/lag/vehicle exemption — legit always-rotate-on-click hits 95% · `[CONFIG]` `ratio>0.95`, `totalAttackTicks>=15`, buffer `>3` hardcoded · `[STYLE]` disabled-by-default but declares `setback=8` |
| combat | AntiVelocity | findings | `[BUG]` `ratioSamples` len 3 but window yields up to 6 → silent overwrite L25/85 · `[BUG]` `sampleIndex` reset in async lambda L54 — corrupts window · `[FP]` no boat/levitation/slow-fall/Riptide/elytra exemption · `[FP]` `avgRatio<0.1` flags wall-pressing players · `[CONFIG]` `VELOCITY_RESPONSE_TICKS/MIN_VELOCITY/frictions/avgRatio/buffer` hardcoded L29-100 |
| combat | AttackFrequency | findings | `[BUG]` `rapidAttacks` persists across combat sessions — accumulates L73 · `[BUG]` `System.currentTimeMillis()` 25ms check — wall-clock jitter L63 · `[FP]` no teleport/lag exemption for `attacksThisTick>1` · `[CONFIG]` `25ms/5/4/2` hardcoded L39-69 · `[STYLE]` asymmetric decay between two flag paths |
| combat | AutoArmor | findings | `[BUG]` no `reward()` ever — VL permanent L35 · `[BUG]` fires on `CLOSE_WINDOW` not armor switch L24 — false timing · `[FP]` no chest/GUI/creative/shift-click exemption · `[CONFIG]` `MIN_SWITCH_DELAY 50`, `>10`, `<3` hardcoded · `[STYLE]` `@CheckData` decay unset → `reward()` no-op |
| combat | AutoClicker | findings | `[BUG]` `currentCPS` reset/increment edge case L50-54 · `[BUG]` 5000ms cleanup window vs 1000ms CPS — variance uses stale data L56-87 · `[FP]` `MAX_LEGIT_CPS 25` flags jitter-clickers · `[CONFIG]` `WINDOW_SIZE 20/MAX_LEGIT_CPS 25/MIN_VARIANCE 2/>18/5000` hardcoded · `[STYLE]` `@CheckData` decay unset |
| combat | Criticals | findings | `[BUG]` `lastDeltaY` updated only on INTERACT_ENTITY L41 — stale between attacks · `[FP]` no onGround/teleport/water exemption · `[FP]` `deltaY>-0.01 && wasFalling` flags land-then-attack L31 · `[CONFIG]` `>5`, `0.01` thresholds hardcoded · `[STYLE]` ignores 1.21.2+ `ATTACK` packet — blind on modern clients |
| combat | FastBow | findings | `[BUG]` `System.currentTimeMillis()` charge window — wall-clock jitter L48/58 · `[BUG]` no reset of `isDrawing`/`bowDrawStart` on item-switch/death/teleport · `[FP]` 120ms tolerance insufficient for >200ms ping · `[FP]` `PLAYER_BLOCK_PLACEMENT` triggers on any right-click — offhand-bow false draw L39 · `[CONFIG]` `MIN_CHARGE_TIME 120`, `>3` hardcoded |
| combat | FastEat | findings | `[BUG]` `isConsumable` string match `contains("_apple")` double-matches golden apples, fragile L82 · `[BUG]` no reset of `isUsing`/`useStartTime` on teleport/death/item-switch · `[FP]` `MIN_EAT_TIME 1400` ignores Creative instant-eat · `[CONFIG]` `MIN_EAT_TIME/MIN_DRINK_TIME(unused)/>3/<1` hardcoded |
| combat | Hitboxes | OK | — stub; flagged by `Reach.java` L241 |
| combat | MultiAttack | findings | `[BUG]` entity de-dup L63 — A→B→A in one tick counts 2 not 3 (correct signal, undocumented) · `[FP]` no teleport exemption — `attacksThisTick` leaks to next tick · `[STYLE]` inline-qualified `WrapperPlayClientPlayerFlying.isFlying` L34 |
| combat | MultiInteractA | OK | — mirrors upstream Grim pattern; teleport/spectate/canSkipTicks handled |
| combat | MultiInteractB | findings | `[BUG]` `hasInteracted` reset L59 can clear same-invocation it was set · `[STYLE]` `isTickingReliablyFor(3)` magic `3` should be shared constant L68 · `[STYLE]` no `reward()` path |
| combat | NoSwingAttack | findings | `[BUG]` `sentSwing` cleared every flying packet L62 — ordering-fragile · `[BUG]` 50ms window wall-clock time L48 · `[FP]` spectator-exemption ordering fragile L44 · `[FP]` no 1.8 swing-reorder exemption · `[CONFIG]` `>50/>3/<2` hardcoded |
| combat | Reach | findings | `[BUG]` `reward()` never called — only `cancelBuffer` decays L306 · `[BUG]` `tickBetterReachCheckWithAngle()` no `reward()` on clean hit · `[FP]` `threshold` default `0.0005` extremely tight — sub-pixel interpolation FP L356 · `[STYLE]` `Hitboxes` flagged via cross-check coupling L241 · `[STYLE]` `ATTACK_RANGE_COMPONENT_EXISTS`/`USE_1_8_HITBOX_MARGIN` static final — stale on runtime version change |
| combat | SelfInteract | findings | `[BUG]` TODO L37 camera-entity ID not checked · `[STYLE]` `flagAndAlert() && shouldModifyPackets()` chained — unusual idiom L40 · `[STYLE]` no `reward()`, `@CheckData` decay unset → VL accumulates |
| crash | CrashA | findings | `[BUG]` no `reward()` L19-33 · `[BUG]` `Math.abs(double) > Integer.MAX_VALUE` always false — Y unchecked L26 · `[CONFIG]` `HARD_CODED_BORDER 2.9999999E7D` L12 |
| crash | CrashB | findings | `[BUG]` no `reward()` on valid creative packets · `[FP]` no gamemode-transition-lag exemption |
| crash | CrashC | findings | `[BUG]` no `reward()` L19-34 · `[BUG]` NaN-only rotation packet without `hasPositionChanged` never inspected L21-25 · `[FP]` no `lastPacketWasTeleport` guard |
| crash | CrashD | findings | `[BUG]` `type` never reset on window close — stale `LECTERN` · `[BUG]` `lecternId` never reset to -1 on close — stale ID match L43 · `[STYLE]` no close-window reset |
| crash | CrashE | findings | `[BUG]` no `reward()` L22-27 · `[CONFIG]` `viewDistance<2` magic L23 · `[STYLE]` modifies packet + flags with no `shouldModifyPackets()` guard — mutates for `sac.nomodifypacket` holders L25-26 |
| crash | CrashF | findings | `[BUG]` no `reward()` L21-39 · `[BUG]` negative-button SWAP with negative slot flags twice (overlapping branches L28-35) · `[STYLE]` `slot` computed but used in one branch only |
| crash | CrashG | findings | `[BUG]` no `reward()` across all 3 handlers L23-48 · `[STYLE]` possible ambiguous `BlockPlace` import L6 |
| crash | CrashH | findings | `[BUG]` no `reward()` L20-43 · `[CONFIG]` magic `256/32500/64` L25/35 · `[FP]` operators get 32500-char limit — lag/crash attack surface · `[STYLE]` `flagAndAlert` after `setCancelled` — reversed canonical order |
| crash | CrashI | findings | `[BUG]` no `reward()` L19-39 · `[BUG]` fragile `e.getMessage().substring(27)` — unchecked exception if prefix changes L26-27 · `[CONFIG]` `<-1` boundary undocumented L32 · `[STYLE]` happy/exception path flow unclear |
| elytra | ElytraA | findings | `[BUG]` no `reward()` clean path L19-31 · `[FP]` no teleport/world-change exemption (stale `isGliding`) L24 · `[STYLE]` `onStartGliding` entry-point diverges from other elytra checks |
| elytra | ElytraB | findings | `[BUG]` no `reward()` L27-47 · `[BUG]` `glide=false` set every `isUpdate` packet L46 — `no jump` branch misses if flying packet next tick · `[FP]` `supportsEndTick()` guard leaves `no jump` path unchecked for non-end-tick clients · `[STYLE]` two flag paths share one `setback` |
| elytra | ElytraC | findings | `[BUG]` no `reward()` L38/63 · `[BUG]` `setback=false` set L67 before `if(setback)` L71 — setback unreachable for skip-tick clients · `[STYLE]` `public boolean exempt` field — encapsulation deviation L17 · `[FP]` spectator guard L29-31 doesn't skip rest of packet |
| elytra | ElytraD | findings | `[BUG]` no `reward()` L30-37 · `[FP]` no teleport exemption — stale `canGlide()` inventory state L29 |
| elytra | ElytraE | findings | `[BUG]` no `reward()` L29-36 · `[FP]` permission-change mid-flight transient `isFlying` inconsistency L29 |
| elytra | ElytraF | findings | `[BUG]` no `reward()` L29-36 · `[FP]` `clientClaimsLastOnGround` raw client value — lag FP L29; no ping exemption |
| elytra | ElytraG | findings | `[BUG]` no `reward()` L28-35 · `[FP]` no lower-bound version guard — 1.9-1.15 levitation unchecked L26 · `[FP]` `hasPotionEffect(LEVITATION)` may be 1-tick stale |
| elytra | ElytraH | findings | `[BUG]` no `reward()` L29-36 · `[FP]` `inVehicle()` stale 1 packet on dismount — false flag first post-dismount glide L29 |
| elytra | ElytraI | findings | `[BUG]` no `reward()` L28-35 · `[FP]` `wasTouchingWater` stale 1 tick after exit — FP L25 · `[FP]` no lower-bound version guard — 1.13/1.14 water-glide unchecked L26 |
| exploit | ExploitA | findings | `[CONFIG]` anvil name-length limits `50/35/31/30` hardcoded L38-40 · `[BUG]` no `reward()` clean path · `[STYLE]` single-branch `if` without `reward()` else |
| exploit | ExploitB | findings | `[BUG]` `oldBook` only set on USE_ITEM — server-opened books (villager/`openBook`) cause FP · `[BUG]` no `reward()` anywhere · `[FP]` `checkTitle()` rejects single-space title vanilla permits L241 · `[FP]` USE_ITEM→EDIT_BOOK inventory race nullifies `oldBook` · `[CONFIG]` page/title/count limits `1023/256/15/16/100/50` hardcoded · `[STYLE]` no `reward()` |
| exploit | ExploitC | findings | `[BUG]` raw `int buffer` shadow counter — diverges from `violations` shown to admins · `[BUG]` oversized branch `buffer<=1` returns without decrement L60-68 — one oversized packet silently ignored · `[BUG]` no `reward()` clean path L71 · `[CONFIG]` `MAX_CHANNEL_LENGTH 128`/`MAX_PAYLOAD_SIZE 32767` hardcoded L24-25 · `[FP]` blank-channel handshake FP for modded clients L41 · `[STYLE]` manual `buffer` redundant with base class |
| flight | FlightA | findings | `[BUG]` no `reward()` mid-air L55-60 · `[BUG]` `MIN_FLAG_AIR_TICKS 130 > MAX_AIR_TICKS 120` — outer block dead code for ticks 121-130 · `[BUG]` `MAX_AIR_SPEED` declared L18 never used — speed dimension dead · `[FP]` no liquid/climbing/web/slow-fall/levitation exemption · `[FP]` no lag/ping compensation · `[FP]` no world/dimension-change exemption · `[CONFIG]` `MAX_AIR_SPEED/MAX_AIR_TICKS/MIN_FLAG_AIR_TICKS/setback=10` hardcoded L15-20 · `[STYLE]` `wasOnGround` init `true` inconsistent with `airTicks=0` |
| groundspoof | NoFall | findings | `[BUG]` no `reward()` clean path L20-76 · `[BUG]` packet rewritten `onGround=false` even when `flagAndAlertWithSetback` returns false — corrects ground for exempt players L45-49 · `[BUG]` second `WrapperPlayClientPlayerFlying` double-decodes same packet L53 — state conflict · `[FP]` `isNearGround` expands only by movement threshold — no lag margin L80-81 · `[FP]` no vehicle exemption (1.9+) · `[FP]` no post-teleport-ACK exemption on flag path L41-49 · `[CONFIG]` hardcoded `0.6f`/`0.001f` hitbox L80, `setback=10` L20 · `[STYLE]` `flipPlayerGroundStatus` public mutable field · `[STYLE]` `isNearGround` ignores `onGround` param when false — dead logic L85 |
| misc | APIBypass | findings | `[BUG]` no `reward()` happy path — `warningStreak` only -1/tick, never resets L36-38 · `[BUG]` `violations` never decayed between flags · `[CONFIG]` `0.5` ratio, `5` streak hardcoded L31/33 · `[STYLE]` `@CheckData` decay unset → `reward()` no-op |
| misc | ClientBrand | findings | `[BUG]` Forge-kick logic L72-77 runs on every brand packet, not gated by `!hasBrand` — repeated disconnect checks · `[BUG]` `data.length==0` branch falls through to Forge check misleadingly L45 · `[FP]` modded re-send re-evaluates Forge kick L79 · `[STYLE]` no `configName` — config-disable impossible |
| misc | GhostBlockMitigation | findings | `[BUG]` `posAgainst` vars L31-35 read but never used — dead code · `[BUG]` `resync()` may resync valid placements when only block-placed-against is outside scanned cube · `[FP]` no vehicle/spectator/teleport exemption · `[CONFIG]` `distance<2` clamp L67 prevents `distance=1` · `[STYLE]` `allow=true` default but field name reads inverted-semantic |
| misc | NettyDelay | findings | `[BUG]` no `reward()` on flag path L39 · `[BUG]` asymmetric units — fast tick adds ms-scaled buffer, clean tick decays flat 1 L33/38 · `[FP]` no lag/TPS exemption — server lag spikes accumulate buffer · `[FP]` no world-change/respawn/join exemption · `[CONFIG]` `MIN_DELAY_NS 40ms`, `100`, decay `1`, `>100` hardcoded L15-38 |
| misc | NettyFlood | findings | `[BUG]` no `reward()` on flag path · `[BUG]` `player.cancelledPackets` (cumulative) never reset — permanent flag past 10 · `[CONFIG]` `MAX_CANCELLED_PER_TICK 10` hardcoded L13 · `[STYLE]` `@CheckData` decay unset → `reward()` no-op |
| misc | PayloadCheck | findings | `[BUG]` `flaggedCount` parallel int counter never resets L14 · `[BUG]` oversized branch doesn't increment `flaggedCount` L29-32 — paths independent · `[FP]` only legacy `MC|` channels checked — modern `minecraft:` namespaced large payloads uncaught · `[CONFIG]` `MAX_PAYLOAD_SIZE 32767/8192/3` hardcoded · `[STYLE]` manual counter duplicates base class |
| misc | Post | findings | `[BUG]` `EvictingQueue<>(10)` silently drops oldest flags past 10 L31 · `[FP]` no `lastPacketWasTeleport` exemption on flush path L48 · `[STYLE]` `@CheckData` decay unset — `violations` never decay |
| misc | SpartanDivergence | findings | `[BUG]` `getTotalSACVL()` double→int truncation loses fractional VL L47 · `[BUG]` `ticks` increments while Spartan unavailable — immediate eval with no warm-up · `[FP]` magic diff `30` L35 flags legit cumulative VL · `[CONFIG]` `CHECK_EVERY 200/diff>30/streak>3` hardcoded L16-37 · `[STYLE]` typo `spartaVL` L33 |
| misc | SpartanSync | findings | `[BUG]` `getTotalSACVL()` double-truncation L43 · `[BUG]` `tickCounter` accumulates while Spartan unavailable — immediate eval L25 · `[FP]` `totalSAC>30 && totalSpartan==0` flags legit minor VL L33 · `[FP]` no join-lag/world-change exemption · `[CONFIG]` `CHECK_INTERVAL 100/30/10` hardcoded · `[STYLE]` overlaps SpartanDivergence |
| misc | TransactionOrder | OK | — empty stub, no logic |
| multiactions | FastSwitch | findings | `[BUG]` `reward()` only when `switchCount<2` — exactly 2 never decays L38 · `[BUG]` no `reward()` first-packet path L29 · `[CONFIG]` `MIN_SWITCH_MS 80`, `>5` hardcoded L16/33 · `[FP]` no lag/ping exemption — 80ms reachable high-ping · `[STYLE]` `packet` var constructed never read L26 |
| multiactions | MultiActionsA | findings | `[BUG]` no `reward()` anywhere · `[FP]` no post-teleport exemption (stale item-use state) |
| multiactions | MultiActionsB | findings | `[BUG]` no `reward()` anywhere · `[FP]` no teleport exemption — stale `isSlowedByUsingItem` |
| multiactions | MultiActionsC | findings | `[BUG]` no `reward()` anywhere · `[FP]` `getVerbose` OR-condition fires even when on-ground+swimming L25 · `[FP]` `serverOpenedInventoryThisTick` only same-tick |
| multiactions | MultiActionsD | findings | `[BUG]` no `reward()` anywhere · `[FP]` inherits OR-condition FP L21 · `[FP]` one-tick inventory gap L19 |
| multiactions | MultiActionsE | findings | `[BUG]` no `reward()` anywhere · `[BUG]` `dropping` cleared on any non-async packet L36-37 — real drop+swing can bypass · `[FP]` stale state risk like A/B |
| multiactions | MultiActionsF | findings | `[BUG]` no `reward()` (incl `onPredictionComplete`) · `[BUG]` `block`/`entity` reset together — deferred flag never queued if prediction fires before entity L26-75 · `[STYLE]` `shouldCancel()` in place path not break path L30/69 |
| multiactions | MultiActionsG | findings | `[BUG]` no `reward()` anywhere · `[FP]` `wasVehicleSwitch` known timing FP on boat enter/exit L54 · `[FP]` stale `inVehicle()` after teleport (low risk) · `[STYLE]` `isCheckActive()` duplicated 4× L21-43 |
| packetorder | PacketOrderA | findings | `[BUG]` accumulated `invalid` across unreliable ticks lost without reward/flag L45-50 · `[BUG]` no `reward()` clean path · `[STYLE]` `invalid` counter never decays via `reward()` |
| packetorder | PacketOrderB | findings | `[BUG]` `sentAnimationSinceLastAttack` field-init reads client version before `super(player)` completes L38 · `[FP]` no teleport/world-change exemption · `[BUG]` no `reward()` clean attack path · `[STYLE]` final fields init before `super()` |
| packetorder | PacketOrderC | findings | `[BUG]` no `reward()` · `[FP]` only armor stands exempted L47 — modded entities/pre-1.9 boats not · `[FP]` `sentInteractAt` stale across dimension change → spurious flag |
| packetorder | PacketOrderD | findings | `[BUG]` no `reward()` · `[BUG]` `requiredEntity`/`requiredSneaking` not reset on tick L66-68 — stale mismatch flag · `[FP]` double-guard L34/37 — Via-transformed packet false flag |
| packetorder | PacketOrderE | findings | `[BUG]` no `reward()` · `[BUG]` setback silently dropped if first queued flag's `flagAndAlert` returns false L65-72 · `[STYLE]` `setback` managed separately from queue — unclear flow |
| packetorder | PacketOrderF | findings | `[BUG]` no `reward()` clean path · `[FP]` residual sprint/sneak under lag — no ping buffer · `[CONFIG]` magic `3` in `isTickingReliablyFor(3)` repeated across checks |
| packetorder | PacketOrderG | findings | `[BUG]` no `reward()` · `[BUG]` `canCancel(action)` with `action==null` (CLIENT_STATUS path L28-34) → NPE L50 · `[STYLE]` verbose ternary misses `DROP_ITEM_STACK` label |
| packetorder | PacketOrderH | findings | `[BUG]` no `reward()` — VL unbounded · `[FP]` <1.21.2 sneak-then-sprint same tick at high send rate → false flag |
| packetorder | PacketOrderI | findings | `[BUG]` `START_DIGGING` fall-through (no `break`) into CANCELLED/FINISHED case L91-98 — control-flow hazard · `[BUG]` no `reward()` · `[FP]` `exemptPlacingWhileDigging` doesn't exempt attack path · `[CONFIG]` `exempt-placing-while-digging` default false hardcoded |
| packetorder | PacketOrderJ | findings | `[BUG]` no `reward()` · `[FP]` `isAttacking() && !isInteracting()` fires if entity disappears between packets — no existence check · `[STYLE]` `invalid` counter consistent within file family |
| packetorder | PacketOrderK | findings | `[BUG]` no `reward()` · `[FP]` `isClickingInInventory()` set by any CLICK_WINDOW — fast inventory openers FP · `[BUG]` `canSkipTicks()` branch doesn't cancel packet — inconsistent with non-skip path L41/46 |
| packetorder | PacketOrderL | findings | `[BUG]` no `reward()` · `[FP]` Q-drop then F-swap same tick (legit) false-flags swap branch L42-54 — no grace tick |
| packetorder | PacketOrderM | findings | `[BUG]` no `reward()` · `[BUG]` 2nd USE_ITEM same tick sees `!interacting` again — masks repeated USE-without-INTERACT L44-48 · `[FP]` `BlockFace.OTHER` Via-translation FP |
| packetorder | PacketOrderN | findings | `[BUG]` no `reward()` · `[BUG]` extends `BlockPlaceCheck`; `shouldCancel()` guard may differ from `shouldModifyPackets()` L27 · `[STYLE]` inherits `BlockPlaceCheck` while siblings extend `Check` |
| packetorder | PacketOrderO | findings | `[BUG]` no `reward()` · `[FP]` stale `flying` flag if tick-end sent before packet processed L24-27 · `[FP]` PLAYER_ABILITIES & other intra-tick packets not exempted L32 · `[CONFIG]` VEHICLE_MOVE exemption hardcoded not shared list |
| packetorder | PacketOrderP | findings | `[BUG]` no `reward()` · `[BUG]` `transactions.rem()` removes by value — wrong occurrence on ID reuse · `[FP]` `addRealTimeTaskNext` lambda — `flagAndAlert` on orphaned player if disconnect L43-45 · `[STYLE]` `byte trimTimer` overflow-trim pattern uncommented |
| packetorder | PacketOrderProcessor | findings | `[STYLE]` helper, no `@CheckData`/flag/reward — pure state tracker; extends `Check` for pipeline participation only |
| prediction | DebugHandler | findings | `[STYLE]` extends `AbstractDebugHandler` not `Check` — helper, no flag/reward · `[BUG]` `enabledFlags` L27 never set true — history-queue L85-95 unreachable dead code · `[BUG]` `listeners.removeIf` L112 after send loop — offline players get one extra message · `[STYLE]` `pickColor(offset,offset)` redundant param L47 |
| prediction | GroundSpoof | findings | `[BUG]` `isNewerThanOrEquals(V_1_8)` L23 always true — spectator guard unconditional, false-branch dead · `[FP]` no `reward()` clean path — VL drains slowly via `decay=0.01` only |
| prediction | OffsetHandler | findings | `[BUG]` `removeOffsetLenience()` L78 zeroes lenience granted same tick by `giveOffsetLenienceNextTick()` L41 — defeats purpose · `[STYLE]` `synchronized` wraps `static AtomicInteger` — redundant L43 · `[CONFIG]` thresholds loaded via `onReload` — OK · `[STYLE]` outer condition L39 OR-clause redundant at default config |
| prediction | Phase | findings | `[BUG]` `oldBB` not updated on flag L44 — next tick compares stale pre-phase BB → possible false flag · `[FP]` no vehicle/elytra/boat exemption beyond `predictionComplete.isChecked()` · `[CONFIG]` `setback=1`/`decay=0.005` hardcoded annotation L18 (reloadable via `Check.reload()`) |
| scaffolding | AirLiquidPlace | findings | `[BUG]` no `reward()` clean path L80-85 · `[FP]` creative guard only L59 — no spectator/vehicle exemption · `[CONFIG]` tick-window `<2` magic L67 |
| scaffolding | DuplicateRotPlace | findings | `[BUG]` `rotated==false` skips check with no `reward()` — VL stuck elevated L31-49 · `[FP]` `xDiff<0.0001` extremely tight — FPs on natural identical deltas · `[CONFIG]` `deltaX>2`/`xDiff<0.0001` hardcoded L33/37 |
| scaffolding | FabricatedPlace | findings | `[BUG]` no `reward()` clean path L59-92 · `[CONFIG]` `MAX_DOUBLE_ERROR`/`FLOAT_STEP_AT_ONE`/extended-shape `1.5` effectively unconfigurable magic L24-49 |
| scaffolding | FarPlace | findings | `[BUG]` no `reward()` passing path L42-47 · `[FP]` `hypot(threshold,threshold)` tolerance may be insufficient under lag/0.03 jitter L40 · `[STYLE]` no `reward()` on success |
| scaffolding | InvalidPlaceA | findings | `[BUG]` no `reward()` any path L16-24 · `[STYLE]` no `onReload`/`cancelVL` hook unlike AirLiquidPlace |
| scaffolding | InvalidPlaceB | findings | `[BUG]` face-255 1.8 exemption uses **server** version not **client** version L18-20 — fires wrongly if 1.8 server + 1.9+ clients · `[BUG]` no `reward()` passing path L22-27 · `[STYLE]` no `onReload`/`cancelVL` |
| scaffolding | MultiPlace | findings | `[BUG]` buffered `flags` fired with no `reward()` for innocent ticks L62-72 · `[BUG]` `hasPlaced` resets when spectating another entity L56 — first real placement after un-spectating never triggers · `[FP]` cursor equality flags two different blocks placed same tick L35 · `[CONFIG]` no `cancelVL`/threshold in `onReload` |
| scaffolding | PositionPlace | findings | `[BUG]` no `reward()` passing path L59-62 · `[FP]` `Double.MIN_VALUE` init for `maxEyeHeight` L30 is positive-min not negative-infinity — latent bug · `[FP]` no teleport/world-change exemption |
| scaffolding | RotationPlace | findings | `[BUG]` `flagBuffer` magic `1` reset / `0.1` decay no config L66-69 · `[BUG]` `ignorePost` stays true if `onPostFlyingBlockPlace` never called (packet cancelled) — suppresses next legit check · `[FP]` single post-flying false gates all pre-flying placements 10 ticks L39 · `[CONFIG]` `flagBuffer` reset `1`/decay `0.1` hardcoded |
| scaffolding | ScaffoldA | findings | `[BUG]` `reward()` only when `placeStreak<5` — never called above streak threshold, VL climbs L28-34 · `[FP]` no ladder/water/ice/wall-fill exemption — broad `dist<6.0`/Y`<2.0` · `[FP]` no elytra-glide exemption · `[CONFIG]` `0.1/6.0/2.0/streak 8` hardcoded L26-28 · `[STYLE]` `lastPlaceX/Z` init `0.0` — first placement near origin spurious streak |
| scaffolding | ScaffoldB | findings | `[BUG]` `reward()` only when `towerCount<2` — inconsistent decay L37 · `[BUG]` `towerCount` resets to `1` not `0` L32 — off-by-one inflates sensitivity (3 placements trip `>3`) · `[FP]` no normal tower-jump-bridge exemption · `[FP]` no elytra/vehicle/world-change exemption · `[CONFIG]` `towerCount>3` + decay values hardcoded L29 |

## Pending directories

Not yet audited (subagent quota exhausted — resets 22:20 Asia/Jakarta):
`crossapi` (54), `movement` (17), `sprint` (7), `timer` (5), `vehicle` (6),
`velocity` (3). Re-run audit agents for these 6 directories to complete.

## Next steps

User triages findings. Fixes become follow-up specs grouped by tag / dir.
The pervasive missing-`reward()` pattern likely warrants a single
cross-cutting fix spec.
