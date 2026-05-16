// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

/**
 * Detects impossible rotation values via packet analysis.
 * <p>
 * Valid pitch range is -90.0 to 90.0. Some cheat modules (Derp, HeadRoll, Spin)
 * send impossible pitch values or extremely rapid rotation changes that exceed
 * what is possible with human input. NaN/Infinity rotations also crash/exploit.
 */
@CheckData(name = "BadPacketsAJ", stableKey = "sac.badpackets.aj", description = "Detects impossible rotation values", setback = 15, decay = 0.01)
public class BadPacketsAJ extends Check implements PacketCheck {

    private float lastYaw = 0;
    private float lastPitch = 0;
    private int spinBuffer = 0;

    public BadPacketsAJ(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
        if (!flying.hasRotationChanged()) return;

        float yaw = flying.getLocation().getYaw();
        float pitch = flying.getLocation().getPitch();

        // NaN/Infinity crash exploit
        if (Float.isNaN(yaw) || Float.isNaN(pitch) || Float.isInfinite(yaw) || Float.isInfinite(pitch)) {
            flagAndAlert("NaN/Inf rotation");
            event.setCancelled(true);
            player.onPacketCancel();
            return;
        }

        // Pitch out of range (Derp, HeadRoll)
        if (Math.abs(pitch) > 90.0f) {
            flagAndAlert("pitch=" + String.format("%.2f", pitch));
            event.setCancelled(true);
            player.onPacketCancel();
            return;
        }

        // Detect impossible spin speed (Spin/Derp modules)
        float deltaYaw = Math.abs(yaw - lastYaw);
        if (deltaYaw > 180) deltaYaw = 360 - deltaYaw; // Normalize wrap-around

        float deltaPitch = Math.abs(pitch - lastPitch);

        // Over 200 degrees rotation in a single tick is very suspicious
        if (deltaYaw > 200 || deltaPitch > 170) {
            spinBuffer++;
            if (spinBuffer > 3) {
                flagAndAlert("deltaYaw=" + String.format("%.1f", deltaYaw) + " deltaPitch=" + String.format("%.1f", deltaPitch));
            }
        } else {
            spinBuffer = Math.max(0, spinBuffer - 1);
            if (spinBuffer < 2) reward();
        }

        lastYaw = yaw;
        lastPitch = pitch;
    }
}
