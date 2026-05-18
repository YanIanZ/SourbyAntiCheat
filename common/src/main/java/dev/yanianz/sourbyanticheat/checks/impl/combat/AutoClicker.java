// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

import java.util.LinkedList;

@CheckData(name = "AutoClicker", stableKey = "sac.combat.autoclicker", description = "Detects auto-clicker patterns via CPS analysis", setback = 10, decay = 0.02)
public class AutoClicker extends Check implements PacketCheck {

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private int windowSize = 20;
    // Raised from 25: legit jitter-clickers / drag-clickers can momentarily exceed 25 CPS.
    private int maxLegitCps = 30;
    private int minLegitVariance = 2;
    private int cpsFlagThreshold = 18;
    // Aligned with the 1000ms CPS window so variance is never computed from stale samples.
    private long cleanupWindowMs = 1000;

    private final LinkedList<Long> clickTimestamps = new LinkedList<>();
    private int currentCPS = 0;
    private long lastCPSReset = System.currentTimeMillis();

    public AutoClicker(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.windowSize = config.getIntElse(base + "window-size", 20);
        this.maxLegitCps = config.getIntElse(base + "max-legit-cps", 30);
        this.minLegitVariance = config.getIntElse(base + "min-variance", 2);
        this.cpsFlagThreshold = config.getIntElse(base + "cps-flag-threshold", 18);
        this.cleanupWindowMs = config.getIntElse(base + "cleanup-window-ms", 1000);
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

        if (!isAttack) return;

        long now = System.currentTimeMillis();
        cleanupOldTimestamps(now);

        clickTimestamps.add(now);

        // Roll the CPS counter over cleanly at the window boundary: count this click in the
        // window it starts (reset first, then increment) so a click at the boundary is not lost.
        if (now - lastCPSReset >= 1000) {
            currentCPS = 0;
            lastCPSReset = now;
        }
        currentCPS++;

        if (clickTimestamps.size() >= windowSize) {
            long intervalSum = 0;
            long intervalMin = Long.MAX_VALUE;
            long intervalMax = 0;
            Long previous = null;

            for (long ts : clickTimestamps) {
                if (previous != null) {
                    long diff = ts - previous;
                    intervalSum += diff;
                    if (diff < intervalMin) intervalMin = diff;
                    if (diff > intervalMax) intervalMax = diff;
                }
                previous = ts;
            }

            int sampleSize = clickTimestamps.size() - 1;
            long varianceRange = intervalMax - intervalMin;

            if (currentCPS > maxLegitCps) {
                flagAndAlert("cps=" + currentCPS);
            } else if (currentCPS > cpsFlagThreshold && varianceRange < minLegitVariance && sampleSize >= 10) {
                flagAndAlert("cps=" + currentCPS + " consistent=" + varianceRange + "ms");
            } else {
                reward();
            }
        }
    }

    private void cleanupOldTimestamps(long now) {
        while (!clickTimestamps.isEmpty() && now - clickTimestamps.getFirst() > cleanupWindowMs) {
            clickTimestamps.removeFirst();
        }
    }
}
