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
 * Detects KillAura post-attack rotation patterns — when the player only changes
 * their rotation on tick boundaries that coincide with attacks rather than
 * smooth natural mouse movement.
 * <p>
 * Legit players have continuous rotation across all ticks. KillAura modules
 * often only rotate on attack ticks, leaving "flat" rotation on non-attack ticks.
 */
@CheckData(name = "AimSuspicion", stableKey = "sac.combat.aimsuspicion", description = "Detects suspicious rotation only on attack ticks", setback = 8, decay = 0.025)
public class AimSuspicion extends Check implements PacketCheck {

    private boolean hadRotationThisTick = false;
    private boolean hadAttackThisTick = false;
    private int rotOnAttackOnly = 0;
    private int totalAttackTicks = 0;
    private int buffer = 0;

    public AimSuspicion(SacPlayer player) {
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
            hadAttackThisTick = true;
            return;
        }

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
            hadRotationThisTick = flying.hasRotationChanged();

            // Analyze the previous tick
            if (hadAttackThisTick) {
                totalAttackTicks++;
                if (hadRotationThisTick) {
                    rotOnAttackOnly++;
                }

                // After enough samples, check the ratio
                if (totalAttackTicks >= 15) {
                    double ratio = (double) rotOnAttackOnly / totalAttackTicks;
                    // If rotation happens on >95% of attack ticks but rarely on non-attack ticks,
                    // this indicates automated aim
                    if (ratio > 0.95) {
                        buffer++;
                        if (buffer > 3) {
                            flagAndAlert("ratio=" + String.format("%.2f", ratio) + " samples=" + totalAttackTicks);
                        }
                    } else {
                        buffer = Math.max(0, buffer - 1);
                        if (buffer < 2) reward();
                    }
                    // Reset for next window
                    totalAttackTicks = 0;
                    rotOnAttackOnly = 0;
                }
            }

            hadAttackThisTick = false;
            hadRotationThisTick = false;
        }
    }
}
