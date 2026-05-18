// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

/**
 * Detects KillAura aim snapping — attacking while not looking at the target.
 * <p>
 * When a player attacks, PacketEvents provides us with the player's look direction.
 * We can compare the yaw change between the last movement and the attack to detect
 * sudden aim snaps that exceed human capability followed by a snap-back.
 */
@CheckData(name = "AimSnap", stableKey = "sac.combat.aimsnap", description = "Detects sudden aim snaps during combat", setback = 10, decay = 0.02)
public class AimSnap extends Check implements PacketCheck {

    private float preAttackYaw = 0;
    // Yaw turned through on the way *into* the attack tick — captured at the attack packet
    // while player.yaw/player.lastYaw still hold the correct pair of consecutive ticks.
    private float snapYawIntoAttack = 0;
    private boolean hadAttack = false;
    private int buffer = 0;
    private int flyingPacketsSinceAttack = 0;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private int maxSnapBackPackets = 3;
    private float snapThreshold = 30f;
    private float returnThreshold = 25f;
    private float diffThreshold = 15f;
    private int bufferThreshold = 3;

    public AimSnap(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.maxSnapBackPackets = config.getIntElse(base + "max-snap-back-packets", 3);
        this.snapThreshold = (float) config.getDoubleElse(base + "snap-threshold", 30.0);
        this.returnThreshold = (float) config.getDoubleElse(base + "return-threshold", 25.0);
        this.diffThreshold = (float) config.getDoubleElse(base + "diff-threshold", 15.0);
        this.bufferThreshold = config.getIntElse(base + "buffer-threshold", 3);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        boolean isAttack = false;

        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            isAttack = true;
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            isAttack = packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK;
        }

        if (isAttack) {
            preAttackYaw = player.yaw;
            // Turn-in magnitude: previous tick's yaw vs the attack-tick yaw, captured now
            // before later flying packets advance player.lastYaw and make this stale.
            float turnIn = Math.abs(player.yaw - player.lastYaw);
            if (turnIn > 180) turnIn = 360 - turnIn;
            snapYawIntoAttack = turnIn;
            hadAttack = true;
            flyingPacketsSinceAttack = 0;
            return;
        }

        // Teleport/vehicle exemption — a setback yaw or mount rotation is not a snap.
        if (player.packetStateData.lastPacketWasTeleport || player.inVehicle()) {
            if (hadAttack) {
                // End the suspicious session cleanly so buffer/state cannot drift.
                hadAttack = false;
                buffer = Math.max(0, buffer - 1);
                reward();
            }
            return;
        }

        // On next movement packet, check if the aim snapped back
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) && hadAttack) {
            flyingPacketsSinceAttack++;
            if (flyingPacketsSinceAttack > maxSnapBackPackets) {
                hadAttack = false;
                buffer = Math.max(0, buffer - 1);
                reward();
                return;
            }

            WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
            if (!flying.hasRotationChanged()) {
                if (flyingPacketsSinceAttack >= maxSnapBackPackets) {
                    hadAttack = false;
                    buffer = Math.max(0, buffer - 1);
                    reward();
                }
                return;
            }

            float postYaw = flying.getLocation().getYaw();

            // Snap = the turn-in measured at the attack tick (previous tick -> attack tick).
            float snapYaw = snapYawIntoAttack;

            // Return = how far the player turned back after the attack (attack-tick yaw vs
            // this post-attack movement-packet yaw).
            float returnYaw = Math.abs(postYaw - preAttackYaw);
            if (returnYaw > 180) returnYaw = 360 - returnYaw;

            if (snapYaw > snapThreshold && returnYaw > returnThreshold && Math.abs(snapYaw - returnYaw) < diffThreshold) {
                buffer++;
                if (buffer > bufferThreshold) {
                    flagAndAlert("snap=" + String.format("%.1f", snapYaw) + " return=" + String.format("%.1f", returnYaw));
                }
                hadAttack = false;
            } else if (flyingPacketsSinceAttack >= maxSnapBackPackets) {
                buffer = Math.max(0, buffer - 1);
                reward();
                hadAttack = false;
            }
        }
    }
}
