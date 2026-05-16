# Cross-API Checks Expansion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 7 new cross-API detection checks (CrossFlight, CrossJesus, CrossStep, CrossSpider, CrossNoFall, CrossElytraMove, CrossKillAura) using the existing multi-source agreement pattern.

**Architecture:** 4 new fields on CrossValidationData populated by CheckManagerListener/SacNettyChannelHandler. Each check follows existing pattern — primary PE signal cross-validated with Netty timing + Spartan VL. PostPredictionCheck for prediction-based checks, PacketCheck for packet-timing-based checks.

**Tech Stack:** Java 21, PacketEvents 2.12, Grim API 1.4, Bukkit

---

### Task 1: Add new fields to CrossValidationData

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/crossapi/CrossValidationData.java`

- [ ] **Step 1: Add 4 new fields and reset logic**

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
    public boolean peOnGround;
    public boolean peGliding;
    public double peAttackIntervalMs;

    // Netty layer
    public double nettyPacketRatePerSec;
    public double nettyAvgReadBytesPerPacket;
    public double nettyAvgDelayBetweenPacketsMs;
    public double nettyIntervalVariance;

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

- [ ] **Step 2: Compile**
```bash
./gradlew :common:compileJava
```
Expected: BUILD SUCCESSFUL

---

### Task 2: Populate peOnGround, peGliding, peAttackIntervalMs in CheckManagerListener

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/events/packets/CheckManagerListener.java` lines ~758-763 (crossValidationData section) and add attack tracking near line ~572

- [ ] **Step 1: Add peOnGround and peGliding population after existing pe fields (after line 763)**

In CheckManagerListener.java, inside the `if (!player.inVehicle() && !player.packetStateData.lastPacketWasOnePointSeventeenDuplicate)` block, after the existing crossValidationData population, add:

```java
                player.crossValidationData.peOnGround = onGround;
                player.crossValidationData.peGliding = player.isGliding;
```

- [ ] **Step 2: Add attack interval tracking**

Add a field to track last attack time near the top of `onPacketReceive` handler. Find the first line of `public void onPacketReceive(PacketReceiveEvent event)` (line 386), and insert after `if (player == null) return;`:

```java
        // Track attack interval for cross-API KillAura
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                long now = System.currentTimeMillis();
                if (player.crossValidationData.peAttackIntervalMs > 0) {
                    long interval = now - player.lastAttackTime;
                    player.crossValidationData.peAttackIntervalMs = interval;
                }
                player.lastAttackTime = now;
            }
        }
```

- [ ] **Step 3: Add lastAttackTime field to SacPlayer**

In `SacPlayer.java`, add a public field near other timing fields:

```java
    public long lastAttackTime = 0;
```

- [ ] **Step 4: Add required import to CheckManagerListener**

Add at top:
```java
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
```

- [ ] **Step 5: Compile**
```bash
./gradlew :common:compileJava
```
Expected: BUILD SUCCESSFUL

---

### Task 3: Populate nettyIntervalVariance in SacNettyChannelHandler

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/netty/SacNettyChannelHandler.java`

- [ ] **Step 1: Add interval tracking fields**

Add these fields after line 29 (`private long totalBytesWritten = 0;`):

```java
    private long lastIntervalMs = -1;
    private long intervalVarianceAccum = 0;
    private int intervalSampleCount = 0;
```

- [ ] **Step 2: Compute interval variance in updateCrossValidationData (after line 118)**

Add to `updateCrossValidationData` method, after `data.nettyAvgDelayBetweenPacketsMs = ...`:

```java
                    data.nettyIntervalVariance = intervalSampleCount > 0
                        ? (double) intervalVarianceAccum / intervalSampleCount
                        : 0;
```

- [ ] **Step 3: Track intervals in channelRead (after line 42 `packetCount++;`)**

Add after `packetCount++`:

```java
        if (lastIntervalMs >= 0) {
            long interval = now - lastReadTime;
            long delta = Math.abs(interval - lastIntervalMs);
            intervalVarianceAccum += delta;
            intervalSampleCount++;
        }
        lastIntervalMs = now - lastReadTime;
```

- [ ] **Step 4: Compile**
```bash
./gradlew :common:compileJava
```
Expected: BUILD SUCCESSFUL

---

### Task 4: Create CrossFlight

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossFlight.java`

- [ ] **Step: Write CrossFlight**

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossFlight", configName = "crossflight", decay = 0.05, setback = 25, stableKey = "cross.flight")
public class CrossFlight extends Check implements PostPredictionCheck {

    private double buffer;
    private static final double PREDICTION_THRESHOLD = 1.0;
    private static final double NETTY_RATE_THRESHOLD = 25.0;

    public CrossFlight(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        double offset = player.crossValidationData.offsetFromPrediction;
        boolean notFalling = player.crossValidationData.pePositionDeltaY >= 0;
        boolean predictionFlag = offset > PREDICTION_THRESHOLD && notFalling;

        if (!predictionFlag) {
            buffer = Math.max(0, buffer - 0.05);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Flight");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            buffer += 1.5;
            if (buffer > 3.0) {
                flagAndAlertWithSetback(String.format("offset=%.3f netty=%.1f/s spartan=%s",
                    offset, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
            }
        } else {
            buffer += 0.5;
            if (buffer > 5.0) {
                flagAndAlert(String.format("offset=%.3f (no cross-confirm)", offset));
            }
        }

        reward();
    }
}
```

---

### Task 5: Create CrossJesus

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossJesus.java`

- [ ] **Step: Write CrossJesus**

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossJesus", configName = "crossjesus", decay = 0.05, setback = 15, stableKey = "cross.jesus")
public class CrossJesus extends Check implements PostPredictionCheck {

    private double buffer;
    private static final double OFFSET_THRESHOLD = 0.8;
    private static final double NETTY_RATE_THRESHOLD = 22.0;

    public CrossJesus(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        boolean onWaterSurface = Math.abs(player.crossValidationData.pePositionDeltaY) < 0.01
            && player.wasTouchingWater;
        double offset = player.crossValidationData.offsetFromPrediction;
        boolean predictionFlag = onWaterSurface && offset > OFFSET_THRESHOLD;

        if (!predictionFlag) {
            buffer = Math.max(0, buffer - 0.05);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Jesus");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            buffer += 1.5;
            if (buffer > 3.0) {
                flagAndAlertWithSetback(String.format("offset=%.3f netty=%.1f/s spartan=%s",
                    offset, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
            }
        } else {
            buffer += 0.5;
            if (buffer > 5.0) {
                flagAndAlert(String.format("offset=%.3f (no cross-confirm)", offset));
            }
        }

        reward();
    }
}
```

---

### Task 6: Create CrossStep

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossStep.java`

- [ ] **Step: Write CrossStep**

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "CrossStep", configName = "crossstep", decay = 0.1, setback = 10, stableKey = "cross.step")
public class CrossStep extends Check implements PacketCheck {

    private int stepBuffer;
    private static final double STEP_THRESHOLD = 0.6;
    private static final double NETTY_DELAY_THRESHOLD = 40.0;
    private static final double NETTY_RATE_THRESHOLD = 18.0;

    public CrossStep(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        if (player.packetStateData.lastPacketWasTeleport) {
            stepBuffer = 0;
            return;
        }

        double deltaY = player.crossValidationData.pePositionDeltaY;
        boolean stepSpike = deltaY > STEP_THRESHOLD;
        boolean notJumping = player.clientVelocity.getY() <= 0;

        if (!stepSpike || !notJumping) {
            stepBuffer = Math.max(0, stepBuffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyAvgDelayBetweenPacketsMs < NETTY_DELAY_THRESHOLD
            && player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Step");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            stepBuffer += 2;
        } else {
            stepBuffer += 1;
        }

        if (stepBuffer > 3) {
            flagAndAlertWithSetback(String.format("dy=%.3f netty=%.1f/s delay=%.1fms spartan=%s",
                deltaY, player.crossValidationData.nettyPacketRatePerSec,
                player.crossValidationData.nettyAvgDelayBetweenPacketsMs, spartanResult.type()));
        }
    }
}
```

---

### Task 7: Create CrossSpider

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossSpider.java`

- [ ] **Step: Write CrossSpider**

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "CrossSpider", configName = "crossspider", decay = 0.1, setback = 10, stableKey = "cross.spider")
public class CrossSpider extends Check implements PacketCheck {

    private int spiderBuffer;
    private static final double MIN_Y_DELTA = 0.15;
    private static final double MAX_Y_DELTA = 0.5;
    private static final double NETTY_RATE_THRESHOLD = 20.0;

    public CrossSpider(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        double deltaY = player.crossValidationData.pePositionDeltaY;
        boolean wallClimbing = deltaY > MIN_Y_DELTA && deltaY < MAX_Y_DELTA
            && player.horizontalCollision;

        if (!wallClimbing) {
            spiderBuffer = Math.max(0, spiderBuffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Spider");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            spiderBuffer += 2;
        } else {
            spiderBuffer += 1;
        }

        if (spiderBuffer > 4) {
            flagAndAlertWithSetback(String.format("dy=%.3f hCol=%s netty=%.1f/s spartan=%s",
                deltaY, player.horizontalCollision,
                player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
```

---

### Task 8: Create CrossNoFall

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossNoFall.java`

- [ ] **Step: Write CrossNoFall**

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossNoFall", configName = "crossnofall", decay = 0.15, setback = 15, stableKey = "cross.nofall")
public class CrossNoFall extends Check implements PostPredictionCheck {

    private double buffer;
    private int airborneTicks;
    private static final double OFFSET_THRESHOLD = 0.3;
    private static final double NETTY_DELAY_THRESHOLD = 50.0;

    public CrossNoFall(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        if (player.crossValidationData.peOnGround) {
            airborneTicks = 0;
        } else {
            airborneTicks++;
        }

        double yOffset = Math.abs(player.crossValidationData.pePositionDeltaY
            - player.crossValidationData.predictedDeltaY);
        boolean groundSpoof = player.crossValidationData.peOnGround
            && airborneTicks > 3
            && yOffset > OFFSET_THRESHOLD;

        if (!groundSpoof) {
            buffer = Math.max(0, buffer - 0.15);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyAvgDelayBetweenPacketsMs < NETTY_DELAY_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "NoFall");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            buffer += 1.5;
            if (buffer > 4.0) {
                flagAndAlertWithSetback(String.format("yOff=%.3f airTicks=%d netty=%.1fms spartan=%s",
                    yOffset, airborneTicks,
                    player.crossValidationData.nettyAvgDelayBetweenPacketsMs, spartanResult.type()));
            }
        } else {
            buffer += 0.5;
            if (buffer > 6.0) {
                flagAndAlert(String.format("yOff=%.3f airTicks=%d (no cross-confirm)",
                    yOffset, airborneTicks));
            }
        }

        reward();
    }
}
```

---

### Task 9: Create CrossElytraMove

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossElytraMove.java`

- [ ] **Step: Write CrossElytraMove**

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "CrossElytraMove", configName = "crosselytramove", decay = 0.05, setback = 12, stableKey = "cross.elytramove")
public class CrossElytraMove extends Check implements PacketCheck {

    private int elytraBuffer;
    private static final double SPEED_THRESHOLD = 30.0;
    private static final double NETTY_RATE_THRESHOLD = 20.0;

    public CrossElytraMove(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        boolean gliding = player.crossValidationData.peGliding;
        if (!gliding) {
            elytraBuffer = Math.max(0, elytraBuffer - 1);
            reward();
            return;
        }

        double dx = player.crossValidationData.pePositionDeltaX;
        double dy = player.crossValidationData.pePositionDeltaY;
        double dz = player.crossValidationData.pePositionDeltaZ;
        double speed = Math.sqrt(dx * dx + dy * dy + dz * dz);

        boolean speedFlag = speed > SPEED_THRESHOLD;

        if (!speedFlag) {
            elytraBuffer = Math.max(0, elytraBuffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "ElytraMove");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            elytraBuffer += 2;
        } else {
            elytraBuffer += 1;
        }

        if (elytraBuffer > 3) {
            flagAndAlertWithSetback(String.format("speed=%.1f netty=%.1f/s spartan=%s",
                speed, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
```

---

### Task 10: Create CrossKillAura

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossKillAura.java`

- [ ] **Step: Write CrossKillAura**

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "CrossKillAura", configName = "crosskillaura", decay = 0.01, setback = 50, stableKey = "cross.killaura")
public class CrossKillAura extends Check implements PacketCheck {

    private int auraBuffer;
    private static final double ROTATION_THRESHOLD = 30.0;
    private static final double ATTACK_INTERVAL_THRESHOLD = 200.0;
    private static final double NETTY_RATE_THRESHOLD = 15.0;
    private static final double NETTY_VARIANCE_THRESHOLD = 25.0;

    public CrossKillAura(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
            reward();
            return;
        }

        WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
        if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        double rotSnap = Math.abs(player.crossValidationData.peRotationDeltaYaw);
        double attackInterval = player.crossValidationData.peAttackIntervalMs;
        boolean fastSnap = rotSnap > ROTATION_THRESHOLD
            && attackInterval > 0
            && attackInterval < ATTACK_INTERVAL_THRESHOLD;

        if (!fastSnap) {
            auraBuffer = Math.max(0, auraBuffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD
            && player.crossValidationData.nettyIntervalVariance < NETTY_VARIANCE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "KillAura");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            auraBuffer += 2;
        } else {
            auraBuffer += 1;
        }

        if (auraBuffer > 5) {
            flagAndAlertWithSetback(String.format("yaw=%.1f int=%.0fms netty=%.1f/s var=%.1f spartan=%s",
                rotSnap, attackInterval,
                player.crossValidationData.nettyPacketRatePerSec,
                player.crossValidationData.nettyIntervalVariance, spartanResult.type()));
        }
    }
}
```

---

### Task 11: Register all 7 checks in CheckManager

**Files:**
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/manager/CheckManager.java`

- [ ] **Step 1: Add imports (after line 21)**

```java
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossFlight;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossJesus;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossStep;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossSpider;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossNoFall;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossElytraMove;
import dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossKillAura;
```

- [ ] **Step 2: Add to packetChecks map (after CrossTimer registration, line 135)**

```java
                .put(CrossStep.class, new CrossStep(player))
                .put(CrossSpider.class, new CrossSpider(player))
                .put(CrossElytraMove.class, new CrossElytraMove(player))
                .put(CrossKillAura.class, new CrossKillAura(player))
```

- [ ] **Step 3: Add to postPredictionChecks map (after CrossAntiKB registration, line 286)**

```java
                .put(CrossFlight.class, new CrossFlight(player))
                .put(CrossJesus.class, new CrossJesus(player))
                .put(CrossNoFall.class, new CrossNoFall(player))
```

---

### Task 12: Build verification

- [ ] **Step 1: Spotless**
```bash
./gradlew :common:spotlessApply
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Full build**
```bash
./gradlew clean build
```
Expected: BUILD SUCCESSFUL (53 actionable tasks)

- [ ] **Step 3: Commit**
```bash
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/crossapi/CrossValidationData.java
git add common/src/main/java/dev/yanianz/sourbyanticheat/events/packets/CheckManagerListener.java
git add common/src/main/java/dev/yanianz/sourbyanticheat/player/SacPlayer.java
git add common/src/main/java/dev/yanianz/sourbyanticheat/netty/SacNettyChannelHandler.java
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossFlight.java
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossJesus.java
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossStep.java
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossSpider.java
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossNoFall.java
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossElytraMove.java
git add common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossKillAura.java
git add common/src/main/java/dev/yanianz/sourbyanticheat/manager/CheckManager.java
git commit -m "feat: add 7 cross-API checks (Flight,Jesus,Step,Spider,NoFall,ElytraMove,KillAura)"
```
