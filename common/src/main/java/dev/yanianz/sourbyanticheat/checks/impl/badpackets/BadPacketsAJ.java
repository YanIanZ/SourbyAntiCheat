// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import ac.grim.grimac.api.config.ConfigManager;
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

    // Vanilla clamps pitch to [-90, 90]; anything beyond is impossible.
    private static final float MAX_PITCH = 90.0f;

    private float lastYaw = 0;
    private float lastPitch = 0;
    private int spinBuffer = 0;

    private double maxDeltaYaw = 200.0;
    private double maxDeltaPitch = 170.0;
    private int spinBufferThreshold = 3;

    public BadPacketsAJ(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        maxDeltaYaw = config.getDoubleElse(base + "max-delta-yaw", 200.0);
        maxDeltaPitch = config.getDoubleElse(base + "max-delta-pitch", 170.0);
        spinBufferThreshold = config.getIntElse(base + "spin-buffer-threshold", 3);
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
            if (flagAndAlert("NaN/Inf rotation") && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
            return;
        }

        // Pitch out of range (Derp, HeadRoll)
        if (Math.abs(pitch) > MAX_PITCH) {
            if (flagAndAlert("pitch=" + String.format("%.2f", pitch)) && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
            return;
        }

        // Detect impossible spin speed (Spin/Derp modules)
        float deltaYaw = Math.abs(yaw - lastYaw);
        if (deltaYaw > 180) deltaYaw = 360 - deltaYaw; // Normalize wrap-around

        // Pitch is clamped to [-90, 90] and does not wrap, so deltaPitch needs no
        // wrap normalisation — its maximum is 180.
        float deltaPitch = Math.abs(pitch - lastPitch);

        if (deltaYaw > maxDeltaYaw || deltaPitch > maxDeltaPitch) {
            spinBuffer++;
            if (spinBuffer > spinBufferThreshold) {
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
