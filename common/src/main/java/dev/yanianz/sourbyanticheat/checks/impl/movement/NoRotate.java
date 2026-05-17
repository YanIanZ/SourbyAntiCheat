// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.movement;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

/**
 * Detects NoRotate/AntiAim modules — player sending movement packets
 * with position changes but never changing their rotation.
 * <p>
 * Vanilla clients always include rotation when moving significant distances.
 * NoRotate modules lock the head to prevent server-side aim analysis.
 */
@CheckData(name = "NoRotate", stableKey = "sac.movement.norotate", description = "Detects movement without rotation changes", setback = 5, decay = 0.03)
public class NoRotate extends Check implements PacketCheck {

    private int movesWithoutRotation = 0;
    private int buffer = 0;

    public NoRotate(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.inVehicle() || player.isGliding || player.canFly || player.isFlying
                || player.gamemode == GameMode.CREATIVE
                || player.wasTouchingWater) return;

        if (player.isSneaking) {
            movesWithoutRotation = 0;
            buffer = Math.max(0, buffer - 1);
            return;
        }

        WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
        boolean hasPosition = flying.hasPositionChanged();
        boolean hasRotation = flying.hasRotationChanged();

        if (!hasPosition) {
            return;
        }

        double deltaX = Math.abs(player.x - player.lastX);
        double deltaZ = Math.abs(player.z - player.lastZ);
        double horizontalDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (horizontalDist < 0.5) {
            return;
        }

        if (!hasRotation) {
            movesWithoutRotation++;
            if (movesWithoutRotation > 100 && horizontalDist > 0.6) {
                buffer++;
                if (buffer > 10) {
                    flagAndAlert("norot_ticks=" + movesWithoutRotation + " dist=" + String.format("%.2f", horizontalDist));
                }
            }
        } else {
            movesWithoutRotation = Math.max(0, movesWithoutRotation - 2);
            buffer = Math.max(0, buffer - 1);
            if (buffer < 2) reward();
        }
    }
}
