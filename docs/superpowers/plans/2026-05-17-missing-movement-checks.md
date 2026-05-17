# Missing Movement Cross-API Checks Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 7 missing movement cross-API checks: FastFall, CrossJump, CrossTeleport, CrossFastLadder, DerpHead, CrossFastEat, CrossFoodSprint

**Architecture:** All checks follow existing cross-API pattern — primary detection via PacketEvents/CrossValidationData, cross-validated with Netty timing + Spartan VL. No modifications to original Grim checks.

**Tech Stack:** Java 21, PacketEvents 2.12, Grim API 1.4

---

### Task 1: CrossFastLadder

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossFastLadder.java`
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/manager/CheckManager.java`

- [ ] **Step 1: Write CrossFastLadder**

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.nmsutil.Collisions;

@CheckData(name = "CrossFastLadder", configName = "crossfastladder", decay = 0.02, setback = 10, stableKey = "cross.fastladder")
public class CrossFastLadder extends Check implements PacketCheck {

    private int buffer;
    private static final double MAX_LADDER_SPEED = 0.20;
    private static final double NETTY_RATE_THRESHOLD = 18.0;

    public CrossFastLadder(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.canFly || player.isFlying || player.isGliding || player.inVehicle()
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE) return;

        boolean onLadder = Collisions.hasMaterial(player,
                player.boundingBox.copy(),
                data -> data.first().getType() == StateTypes.LADDER
                    || data.first().getType() == StateTypes.VINE);

        if (!onLadder) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        double deltaY = player.y - player.lastY;

        if (deltaY > MAX_LADDER_SPEED && deltaY < 0.5) {
            boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

            SpartanCrossCheck.CrossCheckResult spartanResult =
                SpartanCrossCheck.checkSpartan(player.uuid, "FastLadder");
            boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

            buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
            if (buffer > 3) {
                flagAndAlert(String.format("dY=%.3f netty=%.1f/s spartan=%s",
                    deltaY, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
            }
        } else {
            buffer = Math.max(0, buffer - 1);
            reward();
        }
    }
}
```

- [ ] **Step 2: Register in CheckManager**

Add import:
```java
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossFastLadder;
```

In `packetChecks` map, add after CrossKillAura:
```java
.put(CrossFastLadder.class, new CrossFastLadder(player))
```

- [ ] **Step 3: Compile**

```bash
./gradlew :common:compileJava
```
Expected: BUILD SUCCESSFUL

---

### Task 2: FastFall

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/FastFall.java`

- [ ] **Step 1: Write FastFall**

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "FastFall", configName = "fastfall", decay = 0.02, setback = 10, stableKey = "cross.fastfall")
public class FastFall extends Check implements PostPredictionCheck {

    private double buffer;
    private static final double FALL_THRESHOLD = 0.5;
    private static final double NETTY_RATE_THRESHOLD = 15.0;

    public FastFall(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        if (player.inVehicle() || player.isGliding || player.canFly
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE) return;

        double deltaY = player.crossValidationData.pePositionDeltaY;
        double predictedY = player.crossValidationData.predictedDeltaY;
        double fallExcess = Math.abs(deltaY) - Math.abs(predictedY);

        boolean fastFalling = deltaY < -0.2 && fallExcess > FALL_THRESHOLD;

        if (!fastFalling) {
            buffer = Math.max(0, buffer - 0.02);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "NoFall");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 1.5 : 0.5;
        if (buffer > 3.0) {
            flagAndAlert(String.format("dY=%.3f predY=%.3f excess=%.3f netty=%.1f/s spartan=%s",
                deltaY, predictedY, fallExcess,
                player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
```

- [ ] **Step 2: Register in CheckManager**

Add import:
```java
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.FastFall;
```

In `postPredictionChecks` map, add after CrossFreecam:
```java
.put(FastFall.class, new FastFall(player))
```

- [ ] **Step 3: Compile**

```bash
./gradlew :common:compileJava
```
Expected: BUILD SUCCESSFUL

---

### Task 3: CrossJump

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossJump.java`

- [ ] **Step 1: Write CrossJump**

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;

@CheckData(name = "CrossJump", configName = "crossjump", decay = 0.02, setback = 10, stableKey = "cross.jump")
public class CrossJump extends Check implements PostPredictionCheck {

    private double buffer;
    private static final double JUMP_THRESHOLD = 1.25;
    private static final double NETTY_DELAY_THRESHOLD = 50.0;

    public CrossJump(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        if (player.inVehicle() || player.canFly || player.isGliding
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.JUMP_BOOST)
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.LEVITATION)
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE) return;

        double deltaY = player.crossValidationData.pePositionDeltaY;

        boolean highJump = deltaY > JUMP_THRESHOLD && player.clientVelocity.getY() <= 0.6;

        if (!highJump) {
            buffer = Math.max(0, buffer - 0.02);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyAvgDelayBetweenPacketsMs < NETTY_DELAY_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Step");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 1.5 : 0.5;
        if (buffer > 3.0) {
            flagAndAlert(String.format("dY=%.3f velY=%.3f netty=%.1fms spartan=%s",
                deltaY, player.clientVelocity.getY(),
                player.crossValidationData.nettyAvgDelayBetweenPacketsMs, spartanResult.type()));
        }
    }
}
```

- [ ] **Step 2: Register in CheckManager**

Add import:
```java
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossJump;
```

In `postPredictionChecks` map, add after FastFall:
```java
.put(CrossJump.class, new CrossJump(player))
```

- [ ] **Step 3: Compile**

---

### Task 4: CrossTeleport

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossTeleport.java`

- [ ] **Step 1: Write CrossTeleport**

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossTeleport", configName = "crossteleport", decay = 0.02, setback = 10, stableKey = "cross.teleport")
public class CrossTeleport extends Check implements PostPredictionCheck {

    private int buffer;
    private static final double TELEPORT_DIST_THRESHOLD = 8.0;

    public CrossTeleport(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        if (player.packetStateData.lastPacketWasTeleport) {
            buffer = Math.max(0, buffer - 1);
            return;
        }
        if (player.inVehicle() || player.canFly
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        double dist = Math.sqrt(
            Math.pow(player.x - player.lastX, 2)
            + Math.pow(player.y - player.lastY, 2)
            + Math.pow(player.z - player.lastZ, 2)
        );

        if (dist < TELEPORT_DIST_THRESHOLD) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec < 10.0;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Phase");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 2) {
            flagAndAlert(String.format("dist=%.1f netty=%.1f/s spartan=%s",
                dist, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
```

- [ ] **Step 2: Register in postPredictionChecks map**

```java
.put(CrossTeleport.class, new CrossTeleport(player))
```

---

### Task 5: DerpHead

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/DerpHead.java`

- [ ] **Step 1: Write DerpHead**

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.RotationCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.RotationUpdate;

@CheckData(name = "DerpHead", configName = "derphead", decay = 0.02, setback = 5, stableKey = "cross.derp")
public class DerpHead extends Check implements RotationCheck {

    private int derpTicks;
    private int buffer;

    public DerpHead(SacPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (player.disableGrim) return;

        if (player.inVehicle() || player.compensatedEntities.self.isDead
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR
                || rotationUpdate.isCinematic()) return;

        float pitch = player.pitch;
        boolean unnaturalPitch = Math.abs(pitch) > 80;

        if (unnaturalPitch) {
            derpTicks++;
        } else {
            derpTicks = Math.max(0, derpTicks - 2);
            buffer = Math.max(0, buffer - 1);
            if (buffer < 2) reward();
            return;
        }

        if (derpTicks < 20) return;

        boolean nettyConfirms = player.crossValidationData.nettyIntervalVariance < 10.0;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "IrregularMovements");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 4) {
            flagAndAlert(String.format("pitch=%.1f ticks=%d nettyVar=%.1f spartan=%s",
                pitch, derpTicks,
                player.crossValidationData.nettyIntervalVariance, spartanResult.type()));
        }
    }
}
```

- [ ] **Step 2: Register in rotationChecks map**

In CheckManager, find the `rotationChecks` definition and add:
```java
.put(DerpHead.class, new DerpHead(player))
```

---

### Task 6: CrossFastEat

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossFastEat.java`

- [ ] **Step 1: Write CrossFastEat**

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossFastEat", configName = "crossfasteat", decay = 0.02, setback = 5, stableKey = "cross.fasteat")
public class CrossFastEat extends Check implements PostPredictionCheck {

    private double buffer;
    private long useStartTime = 0;
    private boolean isUsing = false;
    private static final long MIN_EAT_TIME = 1400;
    private static final double NETTY_RATE_THRESHOLD = 18.0;

    public CrossFastEat(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            useStartTime = System.currentTimeMillis();
            isUsing = true;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            var dig = new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging(event);
            if (dig.getAction() == com.github.retrooper.packetevents.protocol.player.DiggingAction.RELEASE_USE_ITEM) {
                isUsing = false;
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR
                || player.inVehicle()) return;

        if (!isUsing || useStartTime == 0) {
            buffer = Math.max(0, buffer - 0.02);
            reward();
            return;
        }

        long elapsed = System.currentTimeMillis() - useStartTime;
        if (elapsed > 5000 || elapsed < 100) {
            isUsing = false;
            return;
        }

        boolean fastEat = elapsed < MIN_EAT_TIME;

        if (!fastEat) {
            buffer = Math.max(0, buffer - 0.02);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "FastEat");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 1.5 : 0.5;
        if (buffer > 3.0) {
            flagAndAlert(String.format("eat=%dms netty=%.1f/s spartan=%s",
                elapsed, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
        isUsing = false;
    }
}
```

- [ ] **Step 2: Register in postPredictionChecks**

```java
.put(CrossFastEat.class, new CrossFastEat(player))
```

---

### Task 7: CrossFoodSprint

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossFoodSprint.java`

- [ ] **Step 1: Write CrossFoodSprint**

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossFoodSprint", configName = "crossfoodsprint", decay = 0.02, setback = 5, stableKey = "cross.foodsprint")
public class CrossFoodSprint extends Check implements PostPredictionCheck {

    private double buffer;
    private boolean isUsingItem = false;
    private static final double SPRINT_SPEED = 0.28;
    private static final double NETTY_RATE_THRESHOLD = 15.0;

    public CrossFoodSprint(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            isUsingItem = true;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            var dig = new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging(event);
            if (dig.getAction() == com.github.retrooper.packetevents.protocol.player.DiggingAction.RELEASE_USE_ITEM) {
                isUsingItem = false;
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        if (player.inVehicle() || player.canFly || player.isGliding
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE) return;

        if (!isUsingItem || !player.isSprinting) {
            buffer = Math.max(0, buffer - 0.02);
            reward();
            return;
        }

        double deltaX = player.x - player.lastX;
        double deltaZ = player.z - player.lastZ;
        double speed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        boolean sprintingWhileUsing = speed > SPRINT_SPEED;

        if (!sprintingWhileUsing) {
            buffer = Math.max(0, buffer - 0.02);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "NoSlowdown");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 1.5 : 0.5;
        if (buffer > 3.0) {
            flagAndAlert(String.format("speed=%.2f netty=%.1f/s spartan=%s",
                speed, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
```

- [ ] **Step 2: Register in postPredictionChecks**

```java
.put(CrossFoodSprint.class, new CrossFoodSprint(player))
```

---

### Task 8: Build and Commit

- [ ] **Step 1: Spotless**
```bash
./gradlew :common:spotlessApply
```

- [ ] **Step 2: Full build**
```bash
./gradlew build -x :bungee:spotlessJava -x :velocity:spotlessJava
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**
```bash
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossFastLadder.java
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/FastFall.java
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossJump.java
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossTeleport.java
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/DerpHead.java
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossFastEat.java
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossFoodSprint.java
git add common/src/main/java/dev/yanianz/sourbyanticheat/manager/CheckManager.java
git commit -m "feat: 7 missing movement cross-API checks"
```
