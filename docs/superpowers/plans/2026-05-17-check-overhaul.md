# SourbyAntiCheat Check Overhaul Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove experimental check system, fix 17 buggy checks, disable 8 flawed checks, add 4 cross-API detection checks, and strengthen SpartanAPI bundle.

**Architecture:** Incremental changes across 4 phases. Phase 1 removes the experimental flag system. Phase 2 fixes bugs in existing checks and disables fundamentally flawed ones. Phase 3 adds CrossValidationEngine and 4 new cross-API checks. Phase 4 strengthens SpartanAPI bundle to work standalone.

**Tech Stack:** Java 17+, Gradle (Kotlin DSL), PacketEvents 2.12.2+, Netty 4.1.85+, Bukkit Paper API 1.21.11+

---

## Phase 1: Remove Experimental System

### Task 1: Remove experimental field from CheckData and Check

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/CheckData.java`
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/Check.java`

- [ ] **Step 1: Remove experimental from CheckData annotation**

In `common/src/main/java/dev/yanianz/sourbyanticheat/checks/CheckData.java`, remove the `experimental()` method:

```java
package dev.yanianz.sourbyanticheat.checks;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CheckData {
    String name();

    String alternativeName() default "UNKNOWN";

    String configName() default "DEFAULT";

    String description() default "No description provided";

    String stableKey();

    double decay() default 0.05;

    double setback() default 25;
}
```

- [ ] **Step 2: Remove experimental field and logic from Check**

In `common/src/main/java/dev/yanianz/sourbyanticheat/checks/Check.java`:

1. Remove the field: `@Getter private boolean experimental;`
2. Remove the constructor line: `this.experimental = checkData.experimental();`
3. Remove from `flag()` method (around line 119): `if (experimental && !player.isExperimentalChecks()) return false;`
4. Remove the import for experimental if no longer used

The constructor should look like:
```java
public Check(final @NotNull SacPlayer player) {
    this.player = Objects.requireNonNull(player);

    final CheckData checkData = this.getClass().getAnnotation(CheckData.class);
    if (checkData != null) {
        this.checkName = checkData.name();
        this.configName = checkData.configName();
        if (this.configName.equals("DEFAULT")) this.configName = this.checkName;
        this.decay = checkData.decay();
        this.setbackVL = checkData.setback();
        this.alternativeName = checkData.alternativeName();
        this.description = checkData.description();
        this.stableKey = checkData.stableKey();
        this.displayName = this.checkName;
    }

    reload();
    applyGlobalConfig();
}
```

The `flag()` method should remove the experimental check block:
```java
public final boolean flag(String verbose) {
    long start = System.nanoTime();
    if (player.disableGrim || exemptPermission)
        return false;

    if (skipForBedrock && GeyserUtil.isBedrockPlayer(player.uuid))
        return false;

    if (FLAG_CHANNEL.fire(player, this, verbose)) {
        CheckPerformance.record(checkName, System.nanoTime() - start);
        return false;
    }

    var spartan = violations >= SpartanCrossCheck.getMinVL()
        ? SpartanCrossCheck.checkSpartan(player.uuid, checkName)
        : SpartanCrossCheck.CrossCheckResult.NOT_AVAILABLE;
    spartanSuffix = spartan.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED
        ? " [SAC+Spartan]" : "";

    player.punishmentManager.handleViolation(this);
    lastViolationTime = System.currentTimeMillis();
    violations++;
    SpartanEventBridge.fireViolation(player, checkName, (int) violations, verbose);
    AutoPunishment.checkAndExecute(player, this);
    CheckPerformance.record(checkName, System.nanoTime() - start);
    return true;
}
```

- [ ] **Step 3: Build to verify compilation**

Run: `./gradlew common:compileJava`
Expected: Compilation errors in all 72 files with `experimental = true` in their @CheckData — that's expected, will be fixed in Task 2.

- [ ] **Step 4: Commit**

```bash
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/CheckData.java common/src/main/java/dev/yanianz/sourbyanticheat/checks/Check.java
git commit -m "refactor: remove experimental field from CheckData and Check"
```

---

### Task 2: Remove `experimental = true` from all @CheckData annotations

**Files:**
- Modify: All 72 check files that have `experimental = true` in their @CheckData annotation

- [ ] **Step 1: Find all files with experimental = true**

Run: `grep -rl "experimental = true" common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/`

This will list all files that need the annotation updated.

- [ ] **Step 2: Remove `experimental = true` from each file**

For each file found in Step 1, edit the @CheckData annotation to remove the `experimental = true` parameter. Example transformation:

Before:
```java
@CheckData(name = "AimSuspicion", experimental = true, decay = 0.1)
```

After:
```java
@CheckData(name = "AimSuspicion", decay = 0.1)
```

The full list of files (from analysis):
- aim: AimSuspicion
- badpackets: BadPacketsAC, AD, AE, H, M, P, R, V, W, X, Z
- breaking: FarBreak, MultiBreak, NoSwingBreak, RotationBreak
- chat: ChatA, ChatC, ChatD
- combat: AimSnap, AntiVelocity, AttackFrequency, AutoArmor, MultiAttack, MultiInteractA, MultiInteractB, NoSwingAttack
- elytra: ElytraD, E, G, H, I
- misc: NettyDelay, NettyFlood
- movement: Blink, InventoryMove, NoRotate, SafeWalk, Tower
- multiactions: FastSwitch, MultiActionsA, B, E, F, G
- packetorder: PacketOrderA, D, E, F, G, H, I, J, K, L, M, N, O, P
- scaffolding: MultiPlace, DuplicateRotPlace
- sprint: SprintB, C, D, E, F, G
- timer: NegativeTimer
- vehicle: VehicleD, E, F

- [ ] **Step 3: Build to verify all compile**

Run: `./gradlew common:compileJava`
Expected: Clean compilation with no errors

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor: remove experimental=true from all CheckData annotations"
```

---

### Task 3: Remove ExperimentalChecksFeature and SacPlayer.experimentalChecks

**Files:**
- Delete: `common/src/main/java/dev/yanianz/sourbyanticheat/manager/player/features/types/ExperimentalChecksFeature.java`
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/player/SacPlayer.java`
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/manager/player/features/FeatureManagerImpl.java` (if it references ExperimentalChecksFeature)

- [ ] **Step 1: Delete ExperimentalChecksFeature**

```bash
rm common/src/main/java/dev/yanianz/sourbyanticheat/manager/player/features/types/ExperimentalChecksFeature.java
```

- [ ] **Step 2: Remove experimentalChecks field from SacPlayer**

In `common/src/main/java/dev/yanianz/sourbyanticheat/player/SacPlayer.java`:

1. Remove the field: `private boolean experimentalChecks = true;` (or similar)
2. Remove the getter: `isExperimentalChecks()`
3. Remove the setter: `setExperimentalChecks(boolean)`
4. Remove any references to `experimentalChecks` in the class

- [ ] **Step 3: Remove ExperimentalChecksFeature registration from FeatureManagerImpl**

In `common/src/main/java/dev/yanianz/sourbyanticheat/manager/player/features/FeatureManagerImpl.java`:

Remove the registration of ExperimentalChecksFeature. Look for something like:
```java
features.add(new ExperimentalChecksFeature());
```
or any import/reference to ExperimentalChecksFeature.

- [ ] **Step 4: Search for any remaining references to experimentalChecks or ExperimentalChecks**

Run: `grep -rn "experimentalChecks\|ExperimentalChecks\|experimental-checks\|experimental_checks" common/src/`

Remove any remaining references in config files, message files, or other code.

- [ ] **Step 5: Build and verify**

Run: `./gradlew build`
Expected: Clean build with no errors

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor: remove ExperimentalChecksFeature and player.experimentalChecks"
```

---

### Task 4: Disable flawed checks via config defaults

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/Check.java`

- [ ] **Step 1: Add disabled defaults for 8 flawed checks**

In `Check.java`, find the `applyGlobalConfig()` method. Add logic to disable the 8 checks by default:

```java
private void applyGlobalConfig() {
    try {
        var cfg = SacAPI.INSTANCE.getConfigManager().getConfig();
        isEnabled = cfg.getBooleanElse("checks.enabled." + checkName, isEnabled);
    } catch (Exception ignored) {}
}
```

We need to set the initial `isEnabled` to `false` for the 8 disabled checks. The simplest approach is to add this to each disabled check's constructor OR add a post-construction step. Since `applyGlobalConfig` runs in the Check constructor, we can set `isEnabled = false` as default for these classes.

For each of these 8 checks, in their constructors (or through the existing mechanism), they should start disabled. The cleanest approach: add an `@CheckData` parameter or set `isEnabled` in the constructor.

Actually, the better approach is a `disabledByDefault` concept. But to keep it simple:

For each disabled check (AimSuspicion, AutoArmor, Blink, SafeWalk, BadPacketsAE, BadPacketsW, NettyDelay, NettyFlood), add to their constructor after `super(player)`:

```java
this.isEnabled = false;
```

Wait — this won't work because `isEnabled` is private. Let me check... Actually from `Check.java`, `isEnabled` has `@Setter`. So we can use it. But we need another approach since the Check constructor calls `reload()` then `applyGlobalConfig()` which would override.

Best approach: Add a method that the 8 disabled checks can override, or add to `applyGlobalConfig`:

```java
private static final Set<String> DISABLED_BY_DEFAULT = Set.of(
    "AimSuspicion", "AutoArmor", "Blink", "SafeWalk",
    "BadPacketsAE", "BadPacketsW", "NettyDelay", "NettyFlood"
);

private void applyGlobalConfig() {
    try {
        var cfg = SacAPI.INSTANCE.getConfigManager().getConfig();
        boolean defaultEnabled = !DISABLED_BY_DEFAULT.contains(checkName);
        isEnabled = cfg.getBooleanElse("checks.enabled." + checkName, defaultEnabled);
    } catch (Exception ignored) {}
}
```

Add this `DISABLED_BY_DEFAULT` set to `Check.java` and modify `applyGlobalConfig()`.

- [ ] **Step 2: Update config files to add disabled entries**

In `common/src/main/resources/config/en.yml` (and all language variants), add:

```yaml
checks:
  enabled:
    AimSuspicion: false
    AutoArmor: false
    Blink: false
    SafeWalk: false
    BadPacketsAE: false
    BadPacketsW: false
    NettyDelay: false
    NettyFlood: false
```

- [ ] **Step 3: Build and verify**

Run: `./gradlew build`
Expected: Clean build

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: disable 8 fundamentally flawed checks by default"
```

---

## Phase 2: Fix Problematic Checks

### Task 5: Fix AimSnap — add exemptions and snap-back tracking

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/combat/AimSnap.java`

- [ ] **Step 1: Read current AimSnap implementation**

Read the file to understand the current logic before modifying.

- [ ] **Step 2: Add teleport and vehicle exemptions**

In the check method that processes rotation, add exemptions:
- Skip check if `player.packetStateData.lastPacketWasTeleport`
- Skip check if `player.inVehicle`
- Skip check if player is in creative/spectator mode

- [ ] **Step 3: Fix rotation skip bypass**

Currently, the check returns early if `!flying.hasRotationChanged()`. Fix by tracking multiple flying packets after the attack to find the snap-back:
- Store the attack yaw
- On subsequent flying packets (with rotation), compute the snap-back
- Allow up to 3 flying packets after attack to find the snap-back rotation

- [ ] **Step 4: Make thresholds configurable**

Move hardcoded thresholds (snapYaw > 30, returnYaw > 25, |snapYaw - returnYaw| < 15) to @CheckData or config:
```java
@CheckData(name = "AimSnap", configName = "aimsnap", decay = 0.15, setback = 25)
```

With config defaults:
```yaml
aimsnap:
  snap-threshold: 30
  return-threshold: 25
  diff-threshold: 15
```

- [ ] **Step 5: Build and verify**

Run: `./gradlew common:compileJava`

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "fix: AimSnap - add exemptions, fix rotation skip bypass, configurable thresholds"
```

---

### Task 6: Fix AntiVelocity — add friction compensation and death reset

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/combat/AntiVelocity.java`

- [ ] **Step 1: Read current AntiVelocity implementation**

- [ ] **Step 2: Add friction compensation**

When computing expected movement post-velocity, apply vanilla friction:
- Ground friction: 0.91x per tick (horizontal)
- Air friction: 0.98x per tick (horizontal)
- Vertical: gravity -0.08 per tick with drag 0.98

- [ ] **Step 3: Add collision awareness**

If the player's bounding box intersects with solid blocks in the direction of velocity, reduce expected movement proportionally.

- [ ] **Step 4: Add death/respawn/teleport reset**

Add reset logic:
- On player death: reset `velocityPending`
- On respawn: reset `velocityPending`
- On teleport: reset `velocityPending`

- [ ] **Step 5: Use multi-tick sampling**

Instead of checking ratio at a single point, track the player's movement over 3-5 ticks post-velocity and compute average ratio. This reduces false positives from single-tick anomalies.

- [ ] **Step 6: Build and verify**

Run: `./gradlew common:compileJava`

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "fix: AntiVelocity - friction compensation, collision awareness, death resets, multi-tick sampling"
```

---

### Task 7: Fix MultiInteractA and MultiInteractB

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/combat/MultiInteractA.java`
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/combat/MultiInteractB.java`

- [ ] **Step 1: Read both files**

- [ ] **Step 2: Fix MultiInteractA — extract sneaking from ATTACK packet**

Where the ATTACK/SPECTRATE packet handling uses stale `lastSneaking`, extract the actual sneaking state from `action.sneaking` in the packet data.

- [ ] **Step 3: Fix MultiInteractB — replace float equality with epsilon**

Replace `pos.equals(lastPos)` with an epsilon comparison:
```java
private boolean positionEquals(Vector3d a, Vector3d b) {
    return Math.abs(a.x - b.x) < 0.001
        && Math.abs(a.y - b.y) < 0.001
        && Math.abs(a.z - b.z) < 0.001;
}
```

- [ ] **Step 4: Build and verify**

Run: `./gradlew common:compileJava`

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "fix: MultiInteractA sneaking state, MultiInteractB float equality"
```

---

### Task 8: Fix NoSwingAttack — handle ATTACK packet for 1.9+

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/combat/NoSwingAttack.java`

- [ ] **Step 1: Read current implementation**

- [ ] **Step 2: Add ATTACK packet handling**

Add handling for `PacketType.Play.Client.INTERACT_ENTITY` with `ATTACK` action. The check should track when an ATTACK packet is received and verify that a SWING_ANIMATION packet was also sent within the same tick.

- [ ] **Step 3: Build and verify**

Run: `./gradlew common:compileJava`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "fix: NoSwingAttack - handle ATTACK packet for 1.9+ clients"
```

---

### Task 9: Fix InventoryMove — track server-initiated container close

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/movement/InventoryMove.java`

- [ ] **Step 1: Read current implementation**

- [ ] **Step 2: Track server-initiated CLOSE_WINDOW**

Add a `onPacketSend` handler for `PacketType.Play.Server.CLOSE_WINDOW` that clears `hasOpenContainer` when the server closes the inventory. This ensures the check doesn't false-flag when the server closes a container (e.g., distance-based).

- [ ] **Step 3: Build and verify**

Run: `./gradlew common:compileJava`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "fix: InventoryMove - track server-initiated container close"
```

---

### Task 10: Fix NoRotate — increase threshold and add exemptions

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/movement/NoRotate.java`

- [ ] **Step 1: Read current implementation**

- [ ] **Step 2: Increase distance threshold**

Change the distance threshold from 0.1 to 0.3 blocks. Walking in a straight line at sprint speed (5.6 b/s) gives ~0.28 blocks/tick, so 0.3 threshold avoids false positives while still catching obvious NoRotate.

- [ ] **Step 3: Add exemptions**

Add exemptions for:
- Player is gliding/elytra (`player.isGliding`)
- Player is flying (`player.canFly`)
- Player is in creative/spectator mode

- [ ] **Step 4: Build and verify**

Run: `./gradlew common:compileJava`

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "fix: NoRotate - increase threshold to 0.3, add elytra/fly/creative exemptions"
```

---

### Task 11: Fix Tower — add effect exemptions

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/movement/Tower.java`

- [ ] **Step 1: Read current implementation**

- [ ] **Step 2: Add Jump Boost exemption**

Check if player has Jump Boost effect and increase the JUMP_THRESHOLD accordingly:
- Jump Boost I adds 0.5 to jump height
- Jump Boost II adds 0.75
- Each additional level adds more

- [ ] **Step 3: Add Levitation and Slow Falling exemptions**

Skip the check entirely when player has Levitation or Slow Falling effects.

- [ ] **Step 4: Verify block placement**

Ensure the check only flags when the player actually placed a block in the same tick as the jump (confirming scaffold behavior), not just jumping upward.

- [ ] **Step 5: Build and verify**

Run: `./gradlew common:compileJava`

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "fix: Tower - add Jump Boost/Levitation/Slow Falling exemptions, verify block placement"
```

---

### Task 12: Fix BadPackets checks (AC, AD, H, R, X, Z, AE, W)

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/badpackets/BadPacketsAC.java`
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/badpackets/BadPacketsAD.java`
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/badpackets/BadPacketsH.java`
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/badpackets/BadPacketsR.java`
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/badpackets/BadPacketsX.java`
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/badpackets/BadPacketsZ.java`

**BadPacketsAE and BadPacketsW are DISABLED (not fixed).**

- [ ] **Step 1: Read all 6 files**

- [ ] **Step 2: Fix BadPacketsAC — handle transaction ID wrapping**

For 1.17+ clients (Short-based transaction IDs), handle wrapping around Short.MAX_VALUE:
```java
// After computing the difference, account for wrapping
long diff = (id & 0xFFFFL) - (lastId & 0xFFFFL);
boolean backwardInvalid = diff > 0 && diff < 50000; // Normal forward progression
boolean spoofed = diff < 0 && Math.abs(diff) > 50000; // Backward with large gap
```

- [ ] **Step 3: Fix BadPacketsAD — exclude ATTACK action from use check**

In the use-item detection, exclude `INTERACT_ENTITY` with `ATTACK` action to avoid overlap with NoSwing checks. Add 1.7 client version check.

- [ ] **Step 4: Fix BadPacketsH — don't corrupt lastSequence on invalid**

Update `shouldCancel` method:
```java
public boolean shouldCancel(int sequence) {
    int expected = lastSequence + 1;
    boolean invalid = sequence != expected;
    if (invalid) {
        flagAndAlert("expected=" + expected + " got=" + sequence);
    }
    // Only update on valid sequences
    if (!invalid) {
        lastSequence = sequence;
    }
    return invalid && shouldModifyPackets();
}
```

- [ ] **Step 5: Fix BadPacketsR — remove side effects**

Remove entity/piston cleanup calls from the check:
- Remove `player.compensatedEntities.entitiesRemovedThisTick.clear()`
- Remove `player.compensatedWorld.removeInvalidPistonLikeStuff(oldTransId)`
- These should be in the main tick processing, not in a packet check

- [ ] **Step 6: Fix BadPacketsX — reset sprint/sneak on vehicle change**

Add a vehicle change listener or reset `sprint`/`sneak` fields when the player enters/exits a vehicle.

- [ ] **Step 7: Fix BadPacketsZ — add pre-1.21.2 fallback**

For clients older than 1.21.2, reset the `sent` flag on flying packets instead of CLIENT_TICK_END:
```java
if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_21_2)) {
    // Reset on flying packet for older clients
    sent = false;
}
```

- [ ] **Step 8: Build and verify**

Run: `./gradlew common:compileJava`

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "fix: BadPackets AC/AD/H/R/X/Z - transaction wrapping, sequence validation, side effects, vehicle reset, pre-1.21.2 fallback"
```

---

### Task 13: Fix PacketOrder checks (E, I, P)

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/packetorder/PacketOrderE.java`
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/packetorder/PacketOrderI.java`
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/packetorder/PacketOrderP.java`

- [ ] **Step 1: Read all 3 files**

- [ ] **Step 2: Fix PacketOrderE — duplicate verbose, add parentheses**

1. Remove the duplicate `"sprinting=" + player.packetOrderProcessor.isSprinting()` from verbose string
2. Add explicit parentheses around the canSkipTicks/flagAndAlert logic for clarity:
```java
if (player.canSkipTicks()) {
    flags.add(verbose);
} else {
    flagAndAlert(verbose);
}
```

- [ ] **Step 3: Fix PacketOrderI — replace GrimAC import**

Replace `import ac.grim.grimac.api.config.ConfigManager;` with the project's own ConfigManager (check the import in other PacketOrder files for the correct one).

- [ ] **Step 4: Fix PacketOrderP — remove side-effect bundle sending**

Move the `player.user.sendPacket()` call from the check to a packet handling class. The check should only flag, not send packets.

- [ ] **Step 5: Build and verify**

Run: `./gradlew common:compileJava`

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "fix: PacketOrder E/I/P - verbose fix, import fix, remove side-effect"
```

---

### Task 14: Fix SacNettyChannelHandler

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/netty/SacNettyChannelHandler.java`

- [ ] **Step 1: Read current implementation**

- [ ] **Step 2: Fix protocol detection**

Replace the broken `detectProtocolState` method with a simpler version that relies on PacketEvents' protocol state tracking instead of raw byte parsing:
```java
private void detectProtocolState(ByteBuf msg) {
    // Protocol detection is handled by PacketEvents.
    // Only update our internal state based on what PacketEvents tells us.
    // This method is kept as a no-op or minimal implementation.
}
```

- [ ] **Step 3: Increase flood detection sensitivity**

Change `PACKET_FLOOD_THRESHOLD` from 500 to 200 packets/second.

- [ ] **Step 4: Fix exception handling**

In `exceptionCaught`, only close the channel on serious exceptions:
```java
@Override
public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    if (cause instanceof ClosedChannelException || cause instanceof java.io.IOException) {
        // Don't close on expected disconnection exceptions
        return;
    }
    // Log and close on unexpected exceptions
    LogUtil.warn("Netty exception for " + playerName + ": " + cause.getMessage());
    ctx.close();
}
```

- [ ] **Step 5: Build and verify**

Run: `./gradlew common:compileJava`

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "fix: SacNettyChannelHandler - protocol detection, flood threshold, exception handling"
```

---

## Phase 3: Cross-API Detection Checks

### Task 15: Create CrossValidationData class

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/crossapi/CrossValidationData.java`

- [ ] **Step 1: Create the data class**

```java
package dev.yanianz.sourbyanticheat.checks.crossapi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CrossValidationData {

    // PacketEvents layer
    public double pePositionDeltaX;
    public double pePositionDeltaY;
    public double pePositionDeltaZ;
    public double peRotationDeltaYaw;
    public double peRotationDeltaPitch;
    public long pePacketIntervalMs;
    public int peFlyingPacketsPerTick;

    // Netty layer
    public double nettyPacketRatePerSec;
    public double nettyAvgReadBytesPerPacket;
    public double nettyAvgDelayBetweenPacketsMs;

    // Spartan layer
    public int spartanVL;
    public final Map<String, Integer> spartanPerCheckVL = new ConcurrentHashMap<>();
    public double spartanAgreementRate;

    // SACAPI layer (internal prediction)
    public double predictedDeltaX;
    public double predictedDeltaY;
    public double predictedDeltaZ;
    public double offsetFromPrediction;
    public double uncertaintyFactor;

    public void resetTickData() {
        peFlyingPacketsPerTick = 0;
    }

    public void updateSpartanData(int totalVL, Map<String, Integer> perCheckVL, double agreementRate) {
        this.spartanVL = totalVL;
        this.spartanPerCheckVL.clear();
        this.spartanPerCheckVL.putAll(perCheckVL);
        this.spartanAgreementRate = agreementRate;
    }
}
```

- [ ] **Step 2: Add crossValidationData field to SacPlayer**

In `common/src/main/java/dev/yanianz/sourbyanticheat/player/SacPlayer.java`:
```java
import dev.yanianz.sourbyanticheat.checks.crossapi.CrossValidationData;

// Add field:
public final CrossValidationData crossValidationData = new CrossValidationData();
```

- [ ] **Step 3: Build and verify**

Run: `./gradlew common:compileJava`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add CrossValidationData class and SacPlayer field"
```

---

### Task 16: Create CrossSpeed check

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossSpeed.java`

- [ ] **Step 1: Create the CrossSpeed check**

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossSpeed", configName = "crossspeed", decay = 0.05, setback = 25, stableKey = "cross.speed")
public class CrossSpeed extends Check implements PostPredictionCheck {

    private double buffer;
    private static final double PREDICTION_THRESHOLD = 0.5;
    private static final double NETTY_TIMING_ANOMALY = 0.3;

    public CrossSpeed(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        double offset = player.crossValidationData.offsetFromPrediction;
        offset = Math.min(offset, 3.0); // Cap for extreme cases
        player.crossValidationData.offsetFromPrediction = offset;

        // Primary detection: prediction engine offset
        boolean predictionFlag = offset > PREDICTION_THRESHOLD;

        if (!predictionFlag) {
            buffer = Math.max(0, buffer - 0.05);
            reward();
            return;
        }

        // Cross-validate with Netty timing
        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > 22.0
            || player.crossValidationData.nettyAvgDelayBetweenPacketsMs < NETTY_TIMING_ANOMALY * 1000;

        // Cross-validate with Spartan
        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Speed");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        // Flag if prediction is significantly off AND either Netty or Spartan confirms
        if (predictionFlag && (nettyConfirms || spartanConfirms)) {
            buffer += 1.0;
            if (buffer > 3.0) {
                String verbose = String.format("offset=%.3f netty=%.1f/s spartan=%s",
                    offset,
                    player.crossValidationData.nettyPacketRatePerSec,
                    spartanResult.type());
                flagAndAlertWithSetback(verbose);
            }
        } else if (predictionFlag) {
            buffer += 0.5;
            if (buffer > 5.0) {
                flagAndAlert(String.format("offset=%.3f (no cross-confirm)", offset));
            }
        }

        reward();
    }
}
```

- [ ] **Step 2: Register in CheckManager**

In `common/src/main/java/dev/yanianz/sourbyanticheat/manager/CheckManager.java`, add to the `postPredictionChecks` builder:
```java
.put(CrossSpeed.class, new CrossSpeed(player))
```

Add import:
```java
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossSpeed;
```

- [ ] **Step 3: Build and verify**

Run: `./gradlew common:compileJava`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add CrossSpeed check (cross-API speed detection)"
```

---

### Task 17: Create CrossAntiKB check

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossAntiKB.java`

- [ ] **Step 1: Create the CrossAntiKB check**

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossAntiKB", configName = "crossantikb", decay = 0.15, setback = 15, stableKey = "cross.antikb")
public class CrossAntiKB extends Check implements PostPredictionCheck {

    private int consecutiveFlags;
    private static final double RATIO_THRESHOLD = 0.4;

    public CrossAntiKB(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        // Check if player has pending velocity
        var kbHandler = player.checkManager.getPostPredictionCheck(
            dev.yanianz.sourbyanticheat.checks.impl.velocity.KnockbackHandler.class);
        if (kbHandler == null) return;

        boolean hasVelocity = player.firstBukkitKnockback != null
            || player.firstBukkitExplosion != null;
        if (!hasVelocity) {
            consecutiveFlags = 0;
            reward();
            return;
        }

        double actualMovement = Math.sqrt(
            player.crossValidationData.pePositionDeltaX * player.crossValidationData.pePositionDeltaX
            + player.crossValidationData.pePositionDeltaZ * player.crossValidationData.pePositionDeltaZ
        );

        double predictedMovement = Math.sqrt(
            player.crossValidationData.predictedDeltaX * player.crossValidationData.predictedDeltaX
            + player.crossValidationData.predictedDeltaZ * player.crossValidationData.predictedDeltaZ
        );

        if (predictedMovement < 0.01) {
            reward();
            return;
        }

        double ratio = actualMovement / predictedMovement;

        if (ratio < RATIO_THRESHOLD) {
            // Cross-validate with Spartan
            SpartanCrossCheck.CrossCheckResult spartanResult =
                SpartanCrossCheck.checkSpartan(player.uuid, "AntiVelocity");
            boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

            consecutiveFlags++;

            if (spartanConfirms || consecutiveFlags >= 2) {
                String verbose = String.format("ratio=%.2f predicted=%.3f actual=%.3f spartan=%s consecutive=%d",
                    ratio, predictedMovement, actualMovement, spartanResult.type(), consecutiveFlags);
                flagAndAlertWithSetback(verbose);
            }
        } else {
            consecutiveFlags = 0;
            reward();
        }
    }
}
```

- [ ] **Step 2: Register in CheckManager**

Add to `postPredictionChecks`:
```java
.put(CrossAntiKB.class, new CrossAntiKB(player))
```

Add import:
```java
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossAntiKB;
```

- [ ] **Step 3: Build and verify**

Run: `./gradlew common:compileJava`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add CrossAntiKB check (cross-API anti-knockback detection)"
```

---

### Task 18: Create CrossPhase check

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossPhase.java`

- [ ] **Step 1: Create the CrossPhase check**

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.collisions.datatypes.SimpleCollisionBox;
import dev.yanianz.sourbyanticheat.utils.nmsutil.Collisions;

@CheckData(name = "CrossPhase", configName = "crossphase", decay = 0.05, setback = 10, stableKey = "cross.phase")
public class CrossPhase extends Check implements PacketCheck {

    private int phaseBuffer;
    private long lastPacketTime;
    private static final long GAP_THRESHOLD_MS = 500;

    public CrossPhase(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (!isFlying(event.getPacketType())) return;

        if (player.packetStateData.lastPacketWasTeleport) {
            phaseBuffer = 0;
            return;
        }

        long now = System.currentTimeMillis();
        long gap = now - lastPacketTime;
        lastPacketTime = now;

        // Check for packet gap (blink/fakelag)
        boolean gapFlag = gap > GAP_THRESHOLD_MS;

        // Check for phase (inside block)
        SimpleCollisionBox playerBox = player.getBoundingBox();
        boolean insideBlock = Collisions.hasCollision(player, playerBox);

        if (!gapFlag && !insideBlock) {
            phaseBuffer = Math.max(0, phaseBuffer - 1);
            reward();
            return;
        }

        // Cross-validate with Netty timing data
        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec < 15.0
            || gap > GAP_THRESHOLD_MS * 2;

        // Cross-validate with Spartan
        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Phase");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        String verbose = "";
        if (insideBlock) {
            verbose = String.format("inside-block netty=%.1f/s spartan=%s",
                player.crossValidationData.nettyPacketRatePerSec, spartanResult.type());
        } else if (gapFlag) {
            verbose = String.format("gap=%dms netty=%.1f/s spartan=%s",
                gap, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type());
        }

        if (nettyConfirms || spartanConfirms) {
            phaseBuffer += 2;
        } else {
            phaseBuffer += 1;
        }

        if (phaseBuffer > 3) {
            flagAndAlertWithSetback(verbose);
        }
    }
}
```

- [ ] **Step 2: Register in CheckManager**

Add to `packetChecks` builder:
```java
.put(CrossPhase.class, new CrossPhase(player))
```

Add import:
```java
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossPhase;
```

- [ ] **Step 3: Build and verify**

Run: `./gradlew common:compileJava`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add CrossPhase check (cross-API phase/blink detection)"
```

---

### Task 19: Create CrossTimer check

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossTimer.java`

- [ ] **Step 1: Create the CrossTimer check**

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "CrossTimer", configName = "crosstimer", decay = 0.01, setback = 50, stableKey = "cross.timer")
public class CrossTimer extends Check implements PacketCheck {

    private double balance;
    private static final double BALANCE_LIMIT = 20.0;
    private static final double NETTY_RATE_THRESHOLD = 22.0; // slightly above 20 TPS

    public CrossTimer(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (!isUpdate(event.getPacketType())) return;

        // Primary: PacketEvents balance method
        balance += 1.0; // 1 packet = 1 tick expected
        balance -= 1.0; // 1 tick passed

        // Ceiling
        if (balance > BALANCE_LIMIT) balance = BALANCE_LIMIT;

        boolean balanceFlag = balance > 10.0; // threshold for timer

        if (!balanceFlag) {
            if (balance < -5.0) {
                reward();
            }
            return;
        }

        // Cross-validate with Netty timing
        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD
            || player.crossValidationData.nettyAvgDelayBetweenPacketsMs < 45.0; // ~22 TPS rate

        // Cross-validate with Spartan
        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Timer");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        String verbose = String.format("balance=%.1f netty=%.1f/s spartan=%s",
            balance,
            player.crossValidationData.nettyPacketRatePerSec,
            spartanResult.type());

        if (nettyConfirms || spartanConfirms) {
            flagAndAlertWithSetback(verbose);
        } else {
            flagAndAlert(verbose);
        }
    }
}
```

- [ ] **Step 2: Register in CheckManager**

Add to `prePredictionChecks` builder:
```java
.put(CrossTimer.class, new CrossTimer(player))
```

Add import:
```java
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossTimer;
```

- [ ] **Step 3: Build and verify**

Run: `./gradlew common:compileJava`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add CrossTimer check (cross-API timer detection)"
```

---

### Task 20: Wire CrossValidationData updates from packet handlers

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/events/packets/CheckManagerListener.java`
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/netty/SacNettyChannelHandler.java`

- [ ] **Step 1: Update CheckManagerListener to feed PacketEvents data**

In `CheckManagerListener`, during flying packet processing, update the player's CrossValidationData:
```java
// After position update processing:
player.crossValidationData.pePositionDeltaX = player.x - player.lastX;
player.crossValidationData.pePositionDeltaY = player.y - player.lastY;
player.crossValidationData.pePositionDeltaZ = player.z - player.lastZ;
player.crossValidationData.peRotationDeltaYaw = player.yaw - player.lastYaw;
player.crossValidationData.peRotationDeltaPitch = player.pitch - player.lastPitch;
player.crossValidationData.peFlyingPacketsPerTick++;
```

- [ ] **Step 2: Update SacNettyChannelHandler to feed Netty data**

In `channelRead`, update the player's CrossValidationData:
```java
player.crossValidationData.nettyPacketRatePerSec = packetsPerSecond; // already tracked
player.crossValidationData.nettyAvgDelayBetweenPacketsMs = avgDelay; // compute from timing data
player.crossValidationData.nettyAvgReadBytesPerPacket = avgBytes; // compute from byte tracking
```

- [ ] **Step 3: Update CrossValidationData in prediction engine**

In the prediction completion handler (where offset is computed), update:
```java
player.crossValidationData.offsetFromPrediction = offset;
player.crossValidationData.predictedDeltaX = predictedVelocity.getX();
player.crossValidationData.predictedDeltaY = predictedVelocity.getY();
player.crossValidationData.predictedDeltaZ = predictedVelocity.getZ();
player.crossValidationData.uncertaintyFactor = player.uncertaintyHandler.getUncertainty();
```

- [ ] **Step 4: Build and verify**

Run: `./gradlew common:compileJava`

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: wire CrossValidationData updates from packet handlers, Netty, and prediction engine"
```

---

## Phase 4: SpartanAPI Standalone Bundle

### Task 21: Enhance API class with new methods

**Files:**
- Modify: `common/src/main/java/me/vagdedes/spartan/api/API.java`
- Modify: `common/src/main/java/me/vagdedes/spartan/system/Enums.java`

- [ ] **Step 1: Add new API methods**

In `API.java`, add:
```java
public static boolean isCheckEnabled(Player player, Enums.HackType hackType) {
    if (!isRegistered(player)) return false;
    var sp = SacAPI.INSTANCE.getPlayerDataManager().getPlayer(player.getUniqueId());
    if (sp == null) return false;
    Check check = sp.checkManager.getCheck(hackType.getSacCheckClass());
    return check != null && check.isEnabled();
}

public static Map<String, Integer> getPlayerViolationData(Player player) {
    Map<String, Integer> data = new HashMap<>();
    if (!isRegistered(player)) return data;
    var sp = SacAPI.INSTANCE.getPlayerDataManager().getPlayer(player.getUniqueId());
    if (sp == null) return data;
    for (AbstractCheck check : sp.checkManager.allChecks.values()) {
        if (check instanceof Check c) {
            data.put(c.getCheckName(), (int) c.getViolations());
        }
    }
    return data;
}

public static Collection<Player> getOnlineMonitoredPlayers() {
    return SacAPI.INSTANCE.getPlayerDataManager().getOnlinePlayers()
        .stream()
        .map(sp -> {
            try {
                return (Player) sp.platformPlayer.getClass().getMethod("getPlayer").invoke(sp.platformPlayer);
            } catch (Exception e) {
                return null;
            }
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
}
```

- [ ] **Step 2: Add missing HackType mappings in Enums.java**

Add mappings for all SAC check names:
```java
public enum HackType {
    // Combat
    KillAura, HitReach, Speed, Flight, Aim, Scaffold, Timer, Phase,
    Blink, NoSlow, Sprint, Elytra, InventoryMove, AntiVelocity,
    AutoClicker, Criticals, FastBow, FastEat, AutoArmor, NoSwing,
    MultiAttack, MultiInteract, AttackFrequency,
    // Movement
    Jesus, NoFall, FastLadder, Step, SafeWalk, Spider, NoRotate, Tower,
    // Bad Packets
    BadPackets, PacketOrder,
    // Misc
    Crash, Exploit, Chat, Baritone;

    public Class<? extends Check> getSacCheckClass() {
        return HackTypeMapping.getSacCheckClass(this);
    }
}
```

Create `HackTypeMapping` class to map HackType to actual SAC check classes.

- [ ] **Step 3: Build and verify**

Run: `./gradlew common:compileJava`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: enhance SpartanAPI with new methods and HackType mappings"
```

---

### Task 22: Fix SpartanCrossCheck — remove reflection, add caching

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/spartan/SpartanCrossCheck.java`

- [ ] **Step 1: Replace reflection-based getBukkitPlayer with PlatformPlayer API**

```java
private static Object getBukkitPlayer(UUID uuid) {
    try {
        var sp = SacAPI.INSTANCE.getPlayerDataManager().getPlayer(uuid);
        if (sp == null || sp.platformPlayer == null) return null;
        // Use PlatformPlayer's built-in method instead of reflection
        return sp.platformPlayer.getPlayer();  // Assuming PlatformPlayer has getPlayer()
    } catch (Exception e) {
        return null;
    }
}
```

If `PlatformPlayer` doesn't have `getPlayer()`, use `Bukkit.getPlayer(uuid)` directly since the bukkit module already has Bukkit dependency.

- [ ] **Step 2: Add per-check VL caching**

```java
private static final Map<UUID, Map<String, CachedVL>> vlCache = new ConcurrentHashMap<>();
private static final long CACHE_TTL_MS = 5000;

private record CachedVL(int vl, long timestamp) {}

private static int getCachedVL(UUID uuid, String checkType) {
    Map<String, CachedVL> playerCache = vlCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
    CachedVL cached = playerCache.get(checkType);
    if (cached != null && System.currentTimeMillis() - cached.timestamp() < CACHE_TTL_MS) {
        return cached.vl();
    }
    return -1; // cache miss
}

private static void updateCache(UUID uuid, String checkType, int vl) {
    Map<String, CachedVL> playerCache = vlCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
    playerCache.put(checkType, new CachedVL(vl, System.currentTimeMillis()));
}
```

- [ ] **Step 3: Add min-agreement-rate config**

```java
private static double minAgreementRate = 0.6;

public static void init(boolean enabled) {
    // ... existing code ...
    if (spartanAvailable && SacAPI.INSTANCE.getConfigManager() != null) {
        minVL = SacAPI.INSTANCE.getConfigManager().getConfig().getIntElse("spartanapi.cross-check.min-vl", 3);
        minAgreementRate = SacAPI.INSTANCE.getConfigManager().getConfig().getDoubleElse(
            "spartanapi.cross-check.min-agreement-rate", 0.6);
    }
}
```

- [ ] **Step 4: Build and verify**

Run: `./gradlew common:compileJava`

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "fix: SpartanCrossCheck - remove reflection, add VL caching, add min-agreement-rate"
```

---

### Task 23: Fix SpartanEventBridge — remove reflection, add dedup

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/spartan/SpartanEventBridge.java`

- [ ] **Step 1: Replace reflection with direct Bukkit call**

```java
private static Player getBukkitPlayer(SacPlayer sp) {
    try {
        if (sp.platformPlayer == null) return null;
        return sp.platformPlayer.getPlayer();  // PlatformPlayer API
    } catch (Exception e) {
        // Fallback to Bukkit lookup
        return Bukkit.getPlayer(sp.uuid);
    }
}
```

- [ ] **Step 2: Add deduplication**

```java
private static final Map<String, Long> lastFireTime = new ConcurrentHashMap<>();
private static final long DEDUP_WINDOW_MS = 500;

public static void fireViolation(SacPlayer sacPlayer, String checkName, int violations, String verbose) {
    if (!EVENTS_AVAILABLE) return;

    String dedupKey = sacPlayer.uuid + ":" + checkName;
    long now = System.currentTimeMillis();
    Long lastTime = lastFireTime.get(dedupKey);
    if (lastTime != null && now - lastTime < DEDUP_WINDOW_MS) {
        return; // Deduplicate
    }
    lastFireTime.put(dedupKey, now);

    try {
        Player player = getBukkitPlayer(sacPlayer);
        if (player == null) return;

        PlayerViolationEvent event = new PlayerViolationEvent(player, checkName, violations, verbose);
        Bukkit.getPluginManager().callEvent(event);
    } catch (Exception e) {
        LogUtil.warn("Failed to fire Spartan PlayerViolationEvent: " + e.getMessage());
    }
}
```

- [ ] **Step 3: Add SAC event bus firing**

```java
// Also fire on SAC's internal event bus for non-Bukkit consumers
SacAPI.INSTANCE.getEventBus().get(SacViolationEvent.class).fire(sacPlayer, checkName, violations, verbose);
```

This requires creating `SacViolationEvent` in the events package.

- [ ] **Step 4: Build and verify**

Run: `./gradlew common:compileJava`

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "fix: SpartanEventBridge - remove reflection, add dedup, add SAC event bus"
```

---

### Task 24: Add SpartanAPI config section

**Files:**
- Modify: all config YAML files in `common/src/main/resources/config/`

- [ ] **Step 1: Add spartanapi config section to en.yml**

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

- [ ] **Step 2: Add same section to all other language config files**

Apply the same config section to all 13 language config files (zh, nl, tr, fr, ru, ja, pl, it, de, pt, ro, es).

- [ ] **Step 3: Build and verify**

Run: `./gradlew build`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add spartanapi config section to all language files"
```

---

### Task 25: Full build and integration test

**Files:** None (verification only)

- [ ] **Step 1: Clean build**

Run: `./gradlew clean build`
Expected: Clean build with no errors

- [ ] **Step 2: Verify all new checks are registered**

Run: `grep -r "CrossSpeed\|CrossAntiKB\|CrossPhase\|CrossTimer" common/src/main/java/dev/yanianz/sourbyanticheat/manager/CheckManager.java`
Expected: All 4 checks are registered in CheckManager

- [ ] **Step 3: Verify experimental system is fully removed**

Run: `grep -rn "experimental\|ExperimentalChecks" common/src/main/java/`
Expected: No remaining references (except in comments if any)

- [ ] **Step 4: Verify 8 checks are disabled by default**

Run: `grep -rn "DISABLED_BY_DEFAULT" common/src/main/java/dev/yanianz/sourbyanticheat/checks/Check.java`
Expected: The set exists with 8 check names

- [ ] **Step 5: Final commit**

```bash
git add -A
git commit -m "chore: verify full build and integration of check overhaul"
```

---

## Summary

| Phase | Tasks | Description |
|-------|-------|-------------|
| 1 | 1-4 | Remove experimental system |
| 2 | 5-14 | Fix 17 buggy checks, fix Netty handler |
| 3 | 15-20 | CrossValidationData + 4 new cross-API checks |
| 4 | 21-25 | SpartanAPI standalone bundle |
| **Total** | **25 tasks** | |