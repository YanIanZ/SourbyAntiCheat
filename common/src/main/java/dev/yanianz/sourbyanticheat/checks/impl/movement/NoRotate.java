// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.movement;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.nmsutil.Collisions;
import dev.yanianz.sourbyanticheat.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

/**
 * Detects NoRotate/AntiAim modules — player sending movement packets
 * with position changes but never changing their rotation.
 * <p>
 * Vanilla clients always include rotation when moving significant distances.
 * NoRotate modules lock the head to prevent server-side aim analysis.
 * <p>
 * A long run staring at a fixed point (e.g. mining a wall) is legitimate and
 * keeps rotation frozen. To avoid false-flagging that case we require an extra
 * signal: the horizontal movement direction must keep changing while rotation
 * stays frozen — a real player turning corners cannot do that without rotating.
 */
@CheckData(name = "NoRotate", stableKey = "sac.movement.norotate", description = "Detects movement without rotation changes", setback = 5, decay = 0.03)
public class NoRotate extends Check implements PacketCheck {

    private int suspiciousTicks = 0;
    private double lastMoveAngle = Double.NaN;
    private int directionChanges = 0;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double minSpeed = 0.3;
    private int tickThreshold = 30;
    private int bufferThreshold = 3;

    public NoRotate(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.minSpeed = config.getDoubleElse(base + "min-speed", 0.3);
        this.tickThreshold = config.getIntElse(base + "tick-threshold", 30);
        this.bufferThreshold = config.getIntElse(base + "buffer-threshold", 3);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.inVehicle() || player.isGliding || player.canFly || player.isFlying) return;

        WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
        if (!flying.hasPositionChanged()) {
            return;
        }

        double deltaX = player.x - player.lastX;
        double deltaZ = player.z - player.lastZ;
        double horizontalDist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        // Only care about significant movement (0.3 blocks/tick = sprint speed)
        if (horizontalDist < minSpeed) {
            suspiciousTicks = Math.max(0, suspiciousTicks - 1);
            if (suspiciousTicks < bufferThreshold) reward();
            return;
        }

        // Stairs/slabs/ice change movement profile (auto-step, slide) — exempt to
        // avoid false-flagging the natural direction drift they cause.
        boolean autoLevel = Collisions.hasMaterial(player,
                player.boundingBox.copy().expand(0.05).offset(0, -0.5, 0),
                data -> Materials.isStairs(data.first().getType())
                    || Materials.isSlab(data.first().getType())
                    || data.first().getType() == StateTypes.ICE
                    || data.first().getType() == StateTypes.PACKED_ICE
                    || data.first().getType() == StateTypes.BLUE_ICE
                    || data.first().getType() == StateTypes.FROSTED_ICE);
        if (autoLevel) {
            suspiciousTicks = Math.max(0, suspiciousTicks - 1);
            lastMoveAngle = Double.NaN;
            directionChanges = 0;
            if (suspiciousTicks < bufferThreshold) reward();
            return;
        }

        double moveAngle = Math.atan2(deltaZ, deltaX);
        if (!flying.hasRotationChanged()) {
            // Rotation frozen — count a direction change as the extra signal: a real
            // player cannot meaningfully change heading without also rotating.
            if (!Double.isNaN(lastMoveAngle)) {
                double diff = Math.abs(moveAngle - lastMoveAngle);
                if (diff > Math.PI) diff = 2 * Math.PI - diff;
                if (diff > 0.35) directionChanges++;
            }
            suspiciousTicks++;
            // Sustained movement with zero rotation AND repeated heading changes.
            if (suspiciousTicks > tickThreshold && directionChanges > bufferThreshold) {
                flagAndAlert("norot_ticks=" + suspiciousTicks + " dirChanges=" + directionChanges
                        + " dist=" + String.format("%.2f", horizontalDist));
            }
        } else {
            suspiciousTicks = 0;
            directionChanges = 0;
            reward();
        }
        lastMoveAngle = moveAngle;
    }
}
