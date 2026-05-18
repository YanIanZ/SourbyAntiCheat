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
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "Criticals", stableKey = "sac.combat.criticals", description = "Detects critical hit manipulation", setback = 5, decay = 0.02)
public class Criticals extends Check implements PacketCheck {

    private double lastDeltaY = 0;
    private int critFlags = 0;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private int flagThreshold = 5;
    private double deltaYThreshold = 0.01;

    public Criticals(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.flagThreshold = config.getIntElse(base + "flag-threshold", 5);
        this.deltaYThreshold = config.getDoubleElse(base + "delta-y-threshold", 0.01);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Update lastDeltaY every movement tick — otherwise it goes stale between attacks
        // and the fall/land comparison uses motion from many ticks ago.
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            lastDeltaY = player.y - player.lastY;
            return;
        }

        boolean isAttack = false;
        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            // 1.21.2+ clients send the dedicated ATTACK packet — without this the check is
            // blind on modern clients.
            isAttack = true;
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            isAttack = packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK;
        }

        if (!isAttack) return;

        double deltaY = player.y - player.lastY;
        boolean wasFalling = lastDeltaY < -deltaYThreshold;
        // Crit signal: the player was falling, then the fall motion abruptly cancelled
        // on the attack tick (deltaY ~0) — a faked airborne attack.
        boolean fallCancelled = deltaY > -deltaYThreshold;

        // Exempt states where the fall-then-cancel pattern is legitimate or unreliable:
        // grounded (a genuine land-then-attack zeroes fall motion the honest way),
        // gliding, in a vehicle, in water (swimming cancels fall motion), or right after
        // a teleport/setback (motion deltas are not trustworthy).
        boolean exempt = player.onGround || player.isGliding || player.inVehicle()
                || player.wasTouchingWater || player.packetStateData.lastPacketWasTeleport;

        if (wasFalling && fallCancelled && !exempt) {
            critFlags++;
            if (critFlags > flagThreshold) {
                flagAndAlert("dY=" + String.format("%.3f", deltaY) + " flags=" + critFlags);
            }
        } else {
            critFlags = Math.max(0, critFlags - 1);
            if (critFlags < 2) reward();
        }

        lastDeltaY = deltaY;
    }
}
