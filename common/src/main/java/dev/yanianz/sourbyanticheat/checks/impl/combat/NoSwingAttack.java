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

/**
 * KillAura (NoSwing) detection — detects attacking entities without sending an arm swing packet.
 * Vanilla clients ALWAYS send ANIMATION before or on the same tick as INTERACT_ENTITY(ATTACK).
 * Most KillAura/killaura modules skip the swing packet or send it after the attack.
 */
@CheckData(name = "NoSwing", stableKey = "sac.combat.noswing", description = "Detects attacks without arm animation", setback = 10, decay = 0.02)
public class NoSwingAttack extends Check implements PacketCheck {

    private boolean sentSwing = false;
    private long lastSwingTime = 0;
    private int buffer = 0;

    public NoSwingAttack(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            sentSwing = true;
            lastSwingTime = System.currentTimeMillis();
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;
            if (player.inVehicle() || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

            long now = System.currentTimeMillis();

            // A valid swing must come within 50ms before the attack (same tick or just before)
            if (!sentSwing || (now - lastSwingTime) > 50) {
                buffer++;
                if (buffer > 3) {
                    flagAndAlert("swingDelta=" + (now - lastSwingTime) + "ms buffer=" + buffer);
                }
            } else {
                buffer = Math.max(0, buffer - 1);
                if (buffer < 2) reward();
            }

            sentSwing = false;
        }

        // Reset swing on tick
        if (com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            sentSwing = false;
        }
    }
}
