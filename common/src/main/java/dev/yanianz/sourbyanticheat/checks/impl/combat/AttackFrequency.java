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
 * Detects impossible attack patterns:
 * - Attacking at a rate faster than 1 per tick (20 attacks/sec limit)
 * - This catches high-CPS KillAura that bypasses normal CPS checks by timing within packets
 */
@CheckData(name = "AttackFrequency", stableKey = "sac.combat.attackfrequency", description = "Detects impossible attack rate via packet timing", setback = 10, decay = 0.02)
public class AttackFrequency extends Check implements PacketCheck {

    private int attacksThisTick = 0;
    private int buffer = 0;
    private long lastAttackTime = 0;
    private int rapidAttacks = 0;

    public AttackFrequency(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Reset per-tick counter on flying packet
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            if (attacksThisTick > 1) {
                buffer += attacksThisTick - 1;
                if (buffer > 4) {
                    flagAndAlert("perTick=" + attacksThisTick + " buffer=" + buffer);
                }
            } else {
                buffer = Math.max(0, buffer - 1);
                if (buffer < 2) reward();
            }
            attacksThisTick = 0;
            return;
        }

        boolean isAttack = false;

        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            isAttack = true;
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            isAttack = packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK;
        }

        if (!isAttack) return;

        attacksThisTick++;

        long now = System.currentTimeMillis();
        long delta = now - lastAttackTime;

        // Under 25ms between attacks is physically impossible without cheats (< 40 TPS equivalent)
        if (lastAttackTime > 0 && delta < 25) {
            rapidAttacks++;
            if (rapidAttacks > 5) {
                flagAndAlert("delta=" + delta + "ms rapid=" + rapidAttacks);
            }
        } else {
            rapidAttacks = Math.max(0, rapidAttacks - 1);
        }

        lastAttackTime = now;
    }
}
