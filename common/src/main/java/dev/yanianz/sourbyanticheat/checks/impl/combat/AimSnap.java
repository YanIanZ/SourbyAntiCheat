// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.combat;

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
    private float preAttackPitch = 0;
    private boolean hadAttack = false;
    private int buffer = 0;

    public AimSnap(SacPlayer player) {
        super(player);
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
            preAttackPitch = player.pitch;
            hadAttack = true;
            return;
        }

        // On next movement packet, check if the aim snapped back
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) && hadAttack) {
            hadAttack = false;

            WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
            if (!flying.hasRotationChanged()) return;

            float postYaw = flying.getLocation().getYaw();
            float postPitch = flying.getLocation().getPitch();

            // Calculate how far the aim snapped during attack
            float snapYaw = Math.abs(preAttackYaw - player.lastYaw);
            if (snapYaw > 180) snapYaw = 360 - snapYaw;

            // Then how far it snapped back after
            float returnYaw = Math.abs(postYaw - preAttackYaw);
            if (returnYaw > 180) returnYaw = 360 - returnYaw;

            // Both a large snap TO the target and a large snap BACK is highly suspicious
            if (snapYaw > 30 && returnYaw > 25 && Math.abs(snapYaw - returnYaw) < 15) {
                buffer++;
                if (buffer > 3) {
                    flagAndAlert("snap=" + String.format("%.1f", snapYaw) + " return=" + String.format("%.1f", returnYaw));
                }
            } else {
                buffer = Math.max(0, buffer - 1);
                if (buffer < 2) reward();
            }
        }
    }
}
