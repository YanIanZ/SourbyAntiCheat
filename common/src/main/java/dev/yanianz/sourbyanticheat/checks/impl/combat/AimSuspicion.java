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

/**
 * Detects KillAura post-attack rotation patterns — when the player only changes
 * their rotation on tick boundaries that coincide with attacks rather than
 * smooth natural mouse movement.
 * <p>
 * Legit players have continuous rotation across all ticks. KillAura modules
 * often only rotate on attack ticks, leaving "flat" rotation on non-attack ticks.
 * We compare the rotation rate on attack ticks against the rate on non-attack
 * ticks — a player who rotates on (nearly) every attack tick but rarely otherwise
 * is suspicious.
 */
// NOTE: setback=8 is intentional. @CheckData has no `enabled` field — whether this
// check runs is controlled entirely via config (`checks.enabled.AimSuspicion`), so the
// setback value here is independent of the check's enabled/disabled state.
@CheckData(name = "AimSuspicion", stableKey = "sac.combat.aimsuspicion", description = "Detects suspicious rotation only on attack ticks", setback = 8, decay = 0.025)
public class AimSuspicion extends Check implements PacketCheck {

    private boolean hadAttackThisTick = false;
    private int rotOnAttackTicks = 0;
    private int totalAttackTicks = 0;
    private int rotOnNonAttackTicks = 0;
    private int totalNonAttackTicks = 0;
    private int buffer = 0;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double ratioThreshold = 0.95;
    private int totalAttackTicksMin = 15;
    private int bufferThreshold = 3;

    public AimSuspicion(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.ratioThreshold = config.getDoubleElse(base + "ratio-threshold", 0.95);
        this.totalAttackTicksMin = config.getIntElse(base + "total-attack-ticks-min", 15);
        this.bufferThreshold = config.getIntElse(base + "buffer-threshold", 3);
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

        if (isAttack) {
            hadAttackThisTick = true;
            return;
        }

        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        // Teleport/lag/vehicle exemption — a setback rotation or unreliable ticking
        // distorts the per-tick rotation accounting. Drop the sample without analysis.
        if (player.packetStateData.lastPacketWasTeleport || player.inVehicle()
                || !player.isTickingReliablyFor(3)) {
            hadAttackThisTick = false;
            return;
        }

        WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
        // Rotation measured at the tick boundary (this movement packet) for the tick
        // the attack (if any) belongs to.
        boolean hadRotation = flying.hasRotationChanged();

        if (hadAttackThisTick) {
            totalAttackTicks++;
            if (hadRotation) rotOnAttackTicks++;
        } else {
            totalNonAttackTicks++;
            if (hadRotation) rotOnNonAttackTicks++;
        }

        // After enough attack-tick samples, evaluate the window.
        if (totalAttackTicks >= totalAttackTicksMin) {
            double attackRate = (double) rotOnAttackTicks / totalAttackTicks;
            // Non-attack-tick rotation rate; if the player never had a non-attack tick
            // we cannot distinguish, so treat it as fully rotating (no flag).
            double nonAttackRate = totalNonAttackTicks > 0
                    ? (double) rotOnNonAttackTicks / totalNonAttackTicks
                    : 1.0;

            // Suspicious: rotates on almost every attack tick but rarely otherwise.
            if (attackRate > ratioThreshold && nonAttackRate < ratioThreshold) {
                buffer++;
                if (buffer > bufferThreshold) {
                    flagAndAlert("attackRate=" + String.format("%.2f", attackRate)
                            + " nonAttackRate=" + String.format("%.2f", nonAttackRate)
                            + " samples=" + totalAttackTicks);
                }
            } else {
                buffer = Math.max(0, buffer - 1);
                reward();
            }
            // Reset for next window.
            totalAttackTicks = 0;
            rotOnAttackTicks = 0;
            totalNonAttackTicks = 0;
            rotOnNonAttackTicks = 0;
        }

        hadAttackThisTick = false;
    }
}
