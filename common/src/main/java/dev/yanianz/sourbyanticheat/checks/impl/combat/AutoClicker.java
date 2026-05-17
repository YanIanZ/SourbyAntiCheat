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

import java.util.LinkedList;

@CheckData(name = "AutoClicker", stableKey = "sac.combat.autoclicker", description = "Detects auto-clicker patterns via CPS analysis", setback = 10)
public class AutoClicker extends Check implements PacketCheck {

    private static final int WINDOW_SIZE = 20;
    private static final int MAX_LEGIT_CPS = 25;
    private static final int MIN_LEGIT_VARIANCE = 2;

    private final LinkedList<Long> clickTimestamps = new LinkedList<>();
    private int currentCPS = 0;
    private long lastCPSReset = System.currentTimeMillis();

    public AutoClicker(SacPlayer player) {
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

        if (!isAttack) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        long now = System.currentTimeMillis();
        cleanupOldTimestamps(now);

        clickTimestamps.add(now);

        if (now - lastCPSReset >= 1000) {
            currentCPS = 0;
            lastCPSReset = now;
        }
        currentCPS++;

        if (clickTimestamps.size() >= WINDOW_SIZE) {
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
            double avgInterval = (double) intervalSum / sampleSize;
            long varianceRange = intervalMax - intervalMin;

            if (currentCPS > MAX_LEGIT_CPS) {
                flagAndAlert("cps=" + currentCPS);
            } else if (currentCPS > 18 && varianceRange < MIN_LEGIT_VARIANCE && sampleSize >= 10) {
                flagAndAlert("cps=" + currentCPS + " consistent=" + varianceRange + "ms");
            } else {
                reward();
            }
        }
    }

    private void cleanupOldTimestamps(long now) {
        while (!clickTimestamps.isEmpty() && now - clickTimestamps.getFirst() > 5000) {
            clickTimestamps.removeFirst();
        }
    }
}
