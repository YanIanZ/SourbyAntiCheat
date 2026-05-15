// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.flight;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "FlightA", stableKey = "sac.flight.invalid", description = "Detects illegal flight movements", setback = 10)
public class FlightA extends Check implements PacketCheck {

    private static final double MAX_AIR_SPEED = 0.42;
    private static final int MAX_AIR_TICKS = 120;
    private static final int MIN_FLAG_AIR_TICKS = 130;

    private int airTicks = 0;
    private boolean wasOnGround = true;

    public FlightA(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate) return;

        if (player.canFly || player.isFlying || player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR || player.isGliding) {
            airTicks = 0;
            return;
        }

        WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);

        if (flying.isOnGround() || player.inVehicle()) {
            if (airTicks > 0) reward();
            airTicks = 0;
            wasOnGround = true;
            return;
        }

        if (wasOnGround && !flying.isOnGround()) {
            wasOnGround = false;
            airTicks = 1;
        } else if (!wasOnGround) {
            airTicks++;
        }

        if (airTicks > MAX_AIR_TICKS) {
            double deltaY = player.y - player.lastY;
            if (deltaY >= 0 && airTicks > MIN_FLAG_AIR_TICKS) {
                flagAndAlert("airTicks=" + airTicks + " dY=" + String.format("%.3f", deltaY));
            }
        }
    }
}
