// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.groundspoof;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

/**
 * Sustained-descent ground claim: the client reports onGround=true across
 * consecutive position packets while still visibly descending. Real ground
 * arrests the descent within a tick, so a single landing packet won't trip the
 * buffer. Alert-only (setback = -1); a simple packet heuristic alongside the
 * prediction-based {@code GroundSpoof}. Profile/leniency/exemption-aware.
 */
@CheckData(name = "NoFallB", stableKey = "sac.groundspoof.fallclaim",
        description = "Claims onGround while still descending", setback = -1, decay = 0.05)
public class NoFallB extends Check implements PacketCheck {

    private double lastReportedY = Double.NaN;
    private int consecutive = 0;
    private int threshold = 3;
    private double minDrop = 0.1;

    public NoFallB(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String b = getConfigName() + ".";
        threshold = config.getIntElse(b + "consecutive", 3);
        minDrop = config.getDoubleElse(b + "min-drop", 0.1);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.inVehicle() || player.isFlying || player.canFly || player.isGliding) return;
        if (player.packetStateData.lastPacketWasTeleport) return;

        WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
        if (!flying.hasPositionChanged()) return;

        double y = flying.getLocation().getY();
        if (flying.isOnGround() && !Double.isNaN(lastReportedY) && (y - lastReportedY) < -minDrop) {
            consecutive++;
            if (consecutive >= threshold) {
                flagAndAlert("onGround+descending dy=" + String.format("%.3f", y - lastReportedY) + " n=" + consecutive);
            }
        } else {
            if (consecutive > 0) reward();
            consecutive = 0;
        }
        lastReportedY = y;
    }
}
