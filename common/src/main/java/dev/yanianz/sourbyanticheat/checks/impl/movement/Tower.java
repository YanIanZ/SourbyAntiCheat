// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.movement;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

/**
 * Detects Scaffold/Tower downward placement — rapid position changes
 * while placing blocks beneath, characterized by sending position-only
 * packets at a rate and pattern that indicates automated tower building.
 * <p>
 * Vanilla tower-building has natural delays between jumps. Scaffold modules
 * jump and place blocks at inhuman speeds with perfect Y-axis alignment.
 */
@CheckData(name = "Tower", stableKey = "sac.movement.tower", description = "Detects scaffold tower (rapid upward block placement)", setback = 10, decay = 0.02)
public class Tower extends Check implements PacketCheck {

    private int consecutiveJumps = 0;
    private double lastYDelta = 0;
    private long lastJumpTime = 0;
    private int buffer = 0;

    // A standard vanilla jump is ~0.42 blocks on the first tick.
    private double jumpThreshold = 0.35;
    // Minimum time between jumps in ms — 5 ticks = 250ms is the vanilla minimum.
    // The prior 200ms default sat below this and caught legit fast-jumpers.
    private long minJumpIntervalMs = 250;
    private int consecutiveJumpsThreshold = 4;
    private int bufferThreshold = 2;

    public Tower(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.jumpThreshold = config.getDoubleElse(base + "jump-threshold", 0.35);
        this.minJumpIntervalMs = config.getIntElse(base + "min-jump-interval-ms", 250);
        this.consecutiveJumpsThreshold = config.getIntElse(base + "consecutive-jumps-threshold", 4);
        this.bufferThreshold = config.getIntElse(base + "buffer-threshold", 2);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.inVehicle() || player.isFlying || player.canFly || player.isGliding) return;

        // Exempt if player has effects that modify jump height
        if (player.compensatedEntities.self.hasPotionEffect(PotionTypes.JUMP_BOOST)
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.LEVITATION)) return;

        double yDelta = player.y - player.lastY;
        long now = System.currentTimeMillis();
        boolean bufferGrewThisTick = false;

        // Detect jump (positive Y delta above threshold, followed by a previous negative delta)
        if (yDelta > jumpThreshold && lastYDelta < 0) {
            long jumpInterval = now - lastJumpTime;

            if (jumpInterval < minJumpIntervalMs && jumpInterval > 0) {
                consecutiveJumps++;
                if (consecutiveJumps > consecutiveJumpsThreshold) {
                    buffer++;
                    bufferGrewThisTick = true;
                    if (buffer > bufferThreshold) {
                        flagAndAlert("jumps=" + consecutiveJumps + " interval=" + jumpInterval + "ms");
                        lastJumpTime = now;
                        lastYDelta = yDelta;
                        return;
                    }
                }
            } else {
                // Legal jump interval ends the suspicious streak — reset, don't merely decrement.
                consecutiveJumps = 0;
            }
            lastJumpTime = now;
        }

        if (yDelta < -1.0) {
            consecutiveJumps = 0;
        }

        // Decay the buffer on every clean tick (one that did not grow it), not only on a
        // large fall — otherwise an alternating small-negative / large-positive pattern
        // never decays.
        if (!bufferGrewThisTick) {
            buffer = Math.max(0, buffer - 1);
            if (buffer < 1) reward();
        }

        lastYDelta = yDelta;
    }
}
