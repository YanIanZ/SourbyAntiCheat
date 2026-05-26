// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.netty;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.player.SacPlayer;

/**
 * Netty-only flood check: raw inbound packet rate per completed 1s window.
 * Flagged directly from {@code SacNettyChannelHandler} (netty thread, no decode).
 * Alert-only (setback = -1); high default + multi-window buffer to tolerate
 * legitimate bursts (chunk/login).
 */
@CheckData(name = "NettyPacketRate", stableKey = "sac.netty.packetrate",
        description = "Raw inbound packet flood (netty)", setback = -1, decay = 0.05)
public class NettyPacketRate extends Check {

    private int buffer = 0;
    private double maxRate = 500.0;
    private int bufferThreshold = 2;

    public NettyPacketRate(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String b = getConfigName() + ".";
        maxRate = config.getDoubleElse(b + "max-rate", 500.0);
        bufferThreshold = config.getIntElse(b + "buffer", 2);
    }

    /** Fed once per completed 1-second netty window. */
    public void onWindow(double ratePerSec) {
        if (ratePerSec > maxRate) {
            buffer++;
            if (buffer > bufferThreshold) {
                flagAndAlert(String.format("rate=%.0f/s", ratePerSec));
            }
        } else {
            buffer = Math.max(0, buffer - 1);
            if (buffer == 0) reward();
        }
    }
}
