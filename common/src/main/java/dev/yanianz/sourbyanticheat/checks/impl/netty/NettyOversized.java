// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.netty;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.player.SacPlayer;

/**
 * Netty-only oversized-packet guard: a single inbound packet far larger than any
 * legitimate one is a crash/exploit attempt. Flagged directly from
 * {@code SacNettyChannelHandler} (netty thread, no decode). 2 MiB default is well
 * above any real packet, so it keeps a setback.
 */
@CheckData(name = "NettyOversized", stableKey = "sac.netty.oversized",
        description = "Oversized inbound packet (crash attempt)", setback = 10, decay = 0.5)
public class NettyOversized extends Check {

    private int maxBytes = 2_097_152;

    public NettyOversized(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        maxBytes = config.getIntElse(getConfigName() + ".max-bytes", 2_097_152);
    }

    /** Fed with each inbound packet's raw byte size. */
    public void onPacket(int sizeBytes) {
        if (sizeBytes > maxBytes) {
            flagAndAlert("size=" + sizeBytes + "B");
        }
    }
}
