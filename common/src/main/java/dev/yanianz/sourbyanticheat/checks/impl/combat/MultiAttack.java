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
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

/**
 * Detects KillAura multi-attack patterns — attacking multiple unique entities in a single tick.
 * Vanilla players can only attack one entity per tick. KillAura modules often target multiple
 * entities in rapid succession within the same tick window.
 */
@CheckData(name = "MultiAttack", stableKey = "sac.combat.multiattack", description = "Detects attacking multiple entities per tick", setback = 10, decay = 0.025)
public class MultiAttack extends Check implements PacketCheck {

    private int attacksThisTick = 0;
    private int lastAttackedEntity = -1;
    private int buffer = 0;

    public MultiAttack(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Reset on movement tick
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            // Teleport exemption — a setback can batch several attack packets into one tick,
            // making legitimate single-target combat look like a multi-attack. Discarding the
            // count here prevents it leaking into the next tick.
            boolean exempt = player.packetStateData.lastPacketWasTeleport;

            if (attacksThisTick > 1 && !exempt) {
                buffer += attacksThisTick - 1;
                if (buffer > 3) {
                    flagAndAlert("attacks=" + attacksThisTick + " buffer=" + buffer);
                }
            } else {
                buffer = Math.max(0, buffer - 1);
                if (buffer < 2) reward();
            }
            attacksThisTick = 0;
            lastAttackedEntity = -1;
            return;
        }

        boolean isAttack = false;
        int entityId = -1;

        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            isAttack = true;
            entityId = new WrapperPlayClientAttack(event).getEntityId();
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                isAttack = true;
                entityId = packet.getEntityId();
            }
        }

        // Intentional consecutive-duplicate de-dup: only count an attack when the target
        // differs from the immediately previous one. A vanilla A->B->A sequence in one tick
        // therefore counts as 2 attacks — that is the correct signal (the player did switch
        // targets twice). Re-attacking the SAME entity twice in a row (A->A) is collapsed to
        // 1, since that is a single legitimate hit, not a multi-attack.
        if (isAttack && entityId != lastAttackedEntity) {
            attacksThisTick++;
            lastAttackedEntity = entityId;
        }
    }
}
