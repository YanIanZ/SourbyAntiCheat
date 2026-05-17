# Missing Combat Cross-API Checks Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 7 missing combat cross-API checks: CrossReach, CrossCriticals, CrossFastBow, FightBot, ForceField, BackTrack, CrossNoSwing

**Architecture:** Cross-API pattern — primary detection via PacketEvents, cross-validated with Netty timing + Spartan VL. No modifications to original Grim checks.

**Tech Stack:** Java 21, PacketEvents 2.12, Grim API 1.4

---

### Task 1: CrossReach

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossReach.java`
- Modify: `common/src/main/java/dev/yanianz/sourbyanticheat/manager/CheckManager.java`

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.data.packetentity.PacketEntity;

@CheckData(name = "CrossReach", configName = "crossreach", decay = 0.01, setback = 15, stableKey = "cross.reach")
public class CrossReach extends Check implements PacketCheck {

    private int buffer;
    private static final double REACH_MARGIN = 0.5;
    private static final double NETTY_RATE_THRESHOLD = 15.0;

    public CrossReach(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR
                || player.inVehicle()) return;

        WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
        if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        PacketEntity entity = player.compensatedEntities.entityMap.get(interact.getEntityId());
        if (entity == null || entity.isDead) return;

        double maxReach = player.compensatedEntities.self.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);

        double ex = entity.trackedServerPosition.getPos().getX();
        double ey = entity.trackedServerPosition.getPos().getY();
        double ez = entity.trackedServerPosition.getPos().getZ();
        double dist = Math.sqrt(Math.pow(player.x - ex, 2) + Math.pow(player.y - ey, 2) + Math.pow(player.z - ez, 2));

        boolean outOfReach = dist > maxReach + REACH_MARGIN;

        if (!outOfReach) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Reach");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 3) {
            flagAndAlert(String.format("dist=%.2f max=%.2f netty=%.1f/s spartan=%s",
                dist, maxReach, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
```

Register in packetChecks: `.put(CrossReach.class, new CrossReach(player))`

---

### Task 2: CrossCriticals

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossCriticals.java`

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossCriticals", configName = "crosscriticals", decay = 0.02, setback = 10, stableKey = "cross.criticals")
public class CrossCriticals extends Check implements PostPredictionCheck {

    private int buffer;
    private boolean attackedThisTick = false;

    public CrossCriticals(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            attackedThisTick = interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        if (!attackedThisTick) {
            buffer = Math.max(0, buffer - 1);
            reward();
            attackedThisTick = false;
            return;
        }
        attackedThisTick = false;

        if (player.inVehicle() || player.isGliding || player.canFly
                || player.wasTouchingWater || player.compensatedEntities.self.isDead
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.LEVITATION)
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.SLOW_FALLING)
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE) return;

        double deltaY = player.crossValidationData.pePositionDeltaY;
        boolean notFalling = deltaY > -0.01;

        if (!notFalling) {
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Criticals");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 4) {
            flagAndAlert(String.format("dY=%.3f netty=%.1f/s spartan=%s",
                deltaY, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
```

Register in postPredictionChecks.

---

### Task 3: CrossFastBow

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossFastBow.java`

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "CrossFastBow", configName = "crossfastbow", decay = 0.02, setback = 5, stableKey = "cross.fastbow")
public class CrossFastBow extends Check implements PacketCheck {

    private int buffer;
    private long drawStart = 0;
    private boolean isDrawing = false;
    private static final long MIN_CHARGE_TIME = 100;

    public CrossFastBow(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE) return;

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT
                || event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            var hand = player.inventory.getHeldItem();
            if (hand.getType() == ItemTypes.BOW || hand.getType() == ItemTypes.CROSSBOW) {
                drawStart = System.currentTimeMillis();
                isDrawing = true;
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            var dig = new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging(event);
            if (dig.getAction() == com.github.retrooper.packetevents.protocol.player.DiggingAction.RELEASE_USE_ITEM && isDrawing) {
                long charge = System.currentTimeMillis() - drawStart;
                isDrawing = false;

                if (charge < MIN_CHARGE_TIME && charge > 0) {
                    boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > 15.0;
                    SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "FastBow");
                    boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;
                    buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
                    if (buffer > 3) {
                        flagAndAlert(String.format("charge=%dms netty=%.1f/s spartan=%s", charge, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
                    }
                } else {
                    buffer = Math.max(0, buffer - 1);
                    reward();
                }
            }
        }
    }
}
```

Register in packetChecks.

---

### Task 4: FightBot

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/FightBot.java`

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import dev.yanianz.sourbyanticheat.utils.data.packetentity.PacketEntity;

@CheckData(name = "FightBot", configName = "fightbot", decay = 0.01, setback = 15, stableKey = "cross.fightbot")
public class FightBot extends Check implements PostPredictionCheck {

    private int buffer;
    private int perfectAimStreak;
    private int attackedEntity = -1;

    public FightBot(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;
        WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
        if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            attackedEntity = interact.getEntityId();
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        if (attackedEntity < 0) {
            perfectAimStreak = Math.max(0, perfectAimStreak - 1);
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.inVehicle() || player.compensatedEntities.self.isDead) {
            attackedEntity = -1;
            return;
        }

        PacketEntity entity = player.compensatedEntities.entityMap.get(attackedEntity);
        attackedEntity = -1;
        if (entity == null || entity.isDead) return;

        double ex = entity.trackedServerPosition.getPos().getX();
        double ez = entity.trackedServerPosition.getPos().getZ();
        double angleToEntity = Math.toDegrees(Math.atan2(ex - player.x, ez - player.z));
        double yawDelta = Math.abs(player.yaw - angleToEntity);
        double normalizedDelta = yawDelta > 180 ? 360 - yawDelta : yawDelta;

        if (normalizedDelta < 2.0 && Math.abs(player.crossValidationData.peRotationDeltaYaw) > 5.0) {
            perfectAimStreak++;
        } else {
            perfectAimStreak = Math.max(0, perfectAimStreak - 1);
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        if (perfectAimStreak < 5) return;

        boolean nettyConfirms = player.crossValidationData.nettyIntervalVariance < 15.0;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "KillAura");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 4) {
            flagAndAlert(String.format("yawErr=%.1f streak=%d nettyVar=%.1f spartan=%s",
                normalizedDelta, perfectAimStreak, player.crossValidationData.nettyIntervalVariance, spartanResult.type()));
        }
    }
}
```

Register in postPredictionChecks.

---

### Task 5: ForceField

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/ForceField.java`

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

@CheckData(name = "ForceField", configName = "forcefield", decay = 0.02, setback = 10, stableKey = "cross.forcefield")
public class ForceField extends Check implements PacketCheck {

    private int attacksThisTick;
    private int lastEntity;
    private int buffer;

    public ForceField(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        if (com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            if (attacksThisTick > 1) {
                boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > 15.0;
                SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "KillAura");
                boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;
                buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
                if (buffer > 3) {
                    flagAndAlert(String.format("attacks=%d netty=%.1f/s spartan=%s",
                        attacksThisTick, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
                }
            } else {
                buffer = Math.max(0, buffer - 1);
                if (buffer < 2) reward();
            }
            attacksThisTick = 0;
            lastEntity = -1;
            return;
        }

        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;
        WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
        if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        int eid = interact.getEntityId();
        if (eid != lastEntity) {
            attacksThisTick++;
            lastEntity = eid;
        }
    }
}
```

Register in packetChecks.

---

### Task 6: BackTrack

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/BackTrack.java`

```java
package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "BackTrack", configName = "backtrack", decay = 0.02, setback = 10, stableKey = "cross.backtrack")
public class BackTrack extends Check implements PostPredictionCheck {

    private int buffer;
    private boolean attackedThisTick = false;

    public BackTrack(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            attackedThisTick = interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        if (!attackedThisTick) {
            buffer = Math.max(0, buffer - 1);
            reward();
            attackedThisTick = false;
            return;
        }
        attackedThisTick = false;

        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE) return;

        if (player.packetStateData.lastClaimedPosition == null) return;

        double dx = player.x - player.packetStateData.lastClaimedPosition.getX();
        double dz = player.z - player.packetStateData.lastClaimedPosition.getZ();
        double mismatch = Math.sqrt(dx * dx + dz * dz);

        if (mismatch < 1.5) {
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > 15.0;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "Reach");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 3) {
            flagAndAlert(String.format("mismatch=%.1f netty=%.1f/s spartan=%s",
                mismatch, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
```

Register in postPredictionChecks.

---

### Task 7: CrossNoSwing

**Files:**
- Create: `common/src/main/java/dev/yanianz/sourbyanticheat/checks/impl/crossapi/CrossNoSwing.java`

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

@CheckData(name = "CrossNoSwing", configName = "crossnoswing", decay = 0.02, setback = 5, stableKey = "cross.noswing")
public class CrossNoSwing extends Check implements PacketCheck {

    private int buffer;
    private boolean swingSent = false;

    public CrossNoSwing(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (player.inVehicle() || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            swingSent = true;
            return;
        }

        if (com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            swingSent = false;
            return;
        }

        boolean isAttack = false;
        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            isAttack = true;
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            isAttack = interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK;
        }

        if (!isAttack) return;

        if (!swingSent) {
            boolean nettyConfirms = player.crossValidationData.nettyIntervalVariance < 15.0;
            SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "NoSwing");
            boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;
            buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
            if (buffer > 3) {
                flagAndAlert(String.format("nettyVar=%.1f spartan=%s",
                    player.crossValidationData.nettyIntervalVariance, spartanResult.type()));
            }
        } else {
            buffer = Math.max(0, buffer - 1);
            if (buffer < 2) reward();
        }
    }
}
```

Register in packetChecks.

---

### Task 8: Build and Commit

```bash
./gradlew :common:spotlessApply
./gradlew build -x :bungee:spotlessJava -x :velocity:spotlessJava
```
Expected: BUILD SUCCESSFUL

```bash
git add -A
git commit -m "feat: 7 missing combat cross-API checks"
```
