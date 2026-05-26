// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.misc;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow.WindowClickType;

import java.util.LinkedList;

/**
 * Sustained inhuman inventory click rate (chest-stealer / inventory automation).
 * Counts direct PICKUP clicks per rolling second; shift-click (QUICK_MOVE) and
 * hotbar SWAP are exempt (legitimately bursty). Distinct from the burst /
 * packet-order window checks by measuring sustained CPS. Alert-only (setback = -1),
 * profile/leniency/exemption-aware via {@link Check}.
 */
@CheckData(name = "InventoryClicker", stableKey = "sac.misc.invclicker",
        description = "Inhuman sustained inventory click rate", setback = -1, decay = 0.02)
public class InventoryClickerA extends Check implements PacketCheck {

    private final LinkedList<Long> clicks = new LinkedList<>();
    private long windowMs = 1000;
    private int maxCps = 18;
    private int buffer = 0;
    private int flagThreshold = 3;

    public InventoryClickerA(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String b = getConfigName() + ".";
        windowMs = config.getIntElse(b + "window-ms", 1000);
        maxCps = config.getIntElse(b + "max-cps", 18);
        flagThreshold = config.getIntElse(b + "flag-threshold", 3);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) return;

        WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);
        WindowClickType type = click.getWindowClickType();
        // Shift-click and hotbar swaps are legitimately fast — only count direct picks.
        if (type == WindowClickType.QUICK_MOVE || type == WindowClickType.SWAP) return;

        long now = System.currentTimeMillis();
        clicks.addLast(now);
        while (!clicks.isEmpty() && now - clicks.getFirst() > windowMs) clicks.removeFirst();

        if (clicks.size() > maxCps) {
            buffer++;
            if (buffer > flagThreshold) {
                flagAndAlert("invCps=" + clicks.size());
            }
        } else {
            buffer = Math.max(0, buffer - 1);
            if (buffer < 2) reward();
        }
    }
}
