// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.netty;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.player.SacPlayer;

/**
 * Netty-only timing check: bot-perfect inter-packet spacing (near-zero interval
 * variance over many samples) indicates an automated/timer client. Flagged
 * directly from {@code SacNettyChannelHandler} (netty thread, no decode).
 * Alert-only (setback = -1) — proxies can normalize spacing, so tune up if noisy.
 */
@CheckData(name = "NettyUniformTiming", stableKey = "sac.netty.uniform",
        description = "Bot-perfect packet spacing (netty)", setback = -1, decay = 0.05)
public class NettyUniformTiming extends Check {

    private int minSamples = 200;
    private double epsilonMs = 1.0;
    private int buffer = 0;

    public NettyUniformTiming(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String b = getConfigName() + ".";
        minSamples = config.getIntElse(b + "min-samples", 200);
        epsilonMs = config.getDoubleElse(b + "epsilon-ms", 1.0);
    }

    /** Fed with the running average inter-packet interval variance (ms) + sample count. */
    public void onSample(double avgVarianceMs, int samples) {
        if (samples >= minSamples && avgVarianceMs < epsilonMs) {
            buffer++;
            if (buffer > 1) {
                flagAndAlert(String.format("var=%.2fms n=%d", avgVarianceMs, samples));
            }
        } else if (avgVarianceMs >= epsilonMs) {
            buffer = Math.max(0, buffer - 1);
        }
    }
}
