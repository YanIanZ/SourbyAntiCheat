// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.movement;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
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

    // A standard vanilla jump is ~0.42 blocks on the first tick
    private static final double JUMP_THRESHOLD = 0.35;
    // Minimum time between jumps in ms (5 ticks = 250ms minimum for vanilla)
    private static final long MIN_JUMP_INTERVAL = 200;

    public Tower(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.inVehicle() || player.isFlying || player.canFly || player.isGliding) return;

        double yDelta = player.y - player.lastY;
        long now = System.currentTimeMillis();

        // Detect jump (positive Y delta above threshold, followed by a previous negative delta)
        if (yDelta > JUMP_THRESHOLD && lastYDelta < 0) {
            long jumpInterval = now - lastJumpTime;

            if (jumpInterval < MIN_JUMP_INTERVAL && jumpInterval > 0) {
                consecutiveJumps++;
                if (consecutiveJumps > 4) {
                    buffer++;
                    if (buffer > 2) {
                        flagAndAlert("jumps=" + consecutiveJumps + " interval=" + jumpInterval + "ms");
                    }
                }
            } else {
                consecutiveJumps = Math.max(0, consecutiveJumps - 1);
            }
            lastJumpTime = now;
        }

        if (yDelta < -1.0) {
            // Falling significantly, reset
            consecutiveJumps = 0;
            buffer = Math.max(0, buffer - 1);
            if (buffer < 1) reward();
        }

        lastYDelta = yDelta;
    }
}
