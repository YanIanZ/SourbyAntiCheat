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
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR
                || player.inVehicle()) return;

        if (com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            if (attacksThisTick > 1) {
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
            entityId = new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack(event).getEntityId();
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                isAttack = true;
                entityId = packet.getEntityId();
            }
        }

        if (isAttack && entityId != lastAttackedEntity) {
            attacksThisTick++;
            lastAttackedEntity = entityId;
        }
    }
}
