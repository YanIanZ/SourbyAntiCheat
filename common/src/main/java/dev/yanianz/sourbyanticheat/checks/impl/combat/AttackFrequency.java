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
import dev.yanianz.sourbyanticheat.utils.viaversion.ViaVersionUtil;

/**
 * Detects impossible attack patterns:
 * - Attacking at a rate faster than 1 per tick (20 attacks/sec limit)
 * - This catches high-CPS KillAura that bypasses normal CPS checks by timing within packets
 */
@CheckData(name = "AttackFrequency", stableKey = "sac.combat.attackfrequency", description = "Detects impossible attack rate via packet timing", setback = 10, decay = 0.02)
public class AttackFrequency extends Check implements PacketCheck {

    private int attacksThisTick = 0;
    private int buffer = 0;
    // Monotonic timestamp (nanoTime) of the previous attack — immune to wall-clock/NTP jumps.
    private long lastAttackNanos = 0;
    private int rapidAttacks = 0;
    // Flying ticks observed with no attack — used to detect the end of a combat session.
    private int ticksWithoutAttack = 0;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private long minAttackIntervalMs = 25;
    private int rapidAttacksThreshold = 5;
    private int attacksPerTickThreshold = 4;
    private int bufferThreshold = 2;

    // No attack for this many flying ticks ends the combat session.
    private static final int SESSION_END_TICKS = 40;
    private static final double VIA_BACKWARDS_INTERVAL_MULTIPLIER = 2.0;
    private static final int VIA_BACKWARDS_PER_TICK_CAP = 6;

    public AttackFrequency(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.minAttackIntervalMs = config.getIntElse(base + "min-attack-interval", 25);
        this.rapidAttacksThreshold = config.getIntElse(base + "rapid-attacks-threshold", 5);
        this.attacksPerTickThreshold = config.getIntElse(base + "attacks-per-tick-threshold", 4);
        this.bufferThreshold = config.getIntElse(base + "buffer-threshold", 2);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Reset per-tick counter on flying packet
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            // Teleport/lag exemption — a setback or unreliable ticking can batch packets and
            // make a single tick appear to carry several attacks.
            boolean exempt = player.packetStateData.lastPacketWasTeleport
                    || !player.isTickingReliablyFor(3);

            int effectivePerTickThreshold = attacksPerTickThreshold;
            if (ViaVersionUtil.isViaBackwardsPre1_9(player)) {
                effectivePerTickThreshold = VIA_BACKWARDS_PER_TICK_CAP;
            }

            if (attacksThisTick > 1 && !exempt) {
                buffer += attacksThisTick - 1;
                if (buffer > effectivePerTickThreshold) {
                    flagAndAlert("perTick=" + attacksThisTick + " buffer=" + buffer);
                }
            } else {
                buffer = Math.max(0, buffer - 1);
                if (buffer < bufferThreshold) reward();
            }

            // Track combat-session end: after a sustained no-attack gap, reset the
            // cross-tick rapid-attack counter so it does not accumulate across sessions.
            if (attacksThisTick == 0) {
                ticksWithoutAttack++;
                if (ticksWithoutAttack >= SESSION_END_TICKS) {
                    rapidAttacks = 0;
                    lastAttackNanos = 0;
                    ticksWithoutAttack = 0;
                }
            } else {
                ticksWithoutAttack = 0;
            }

            attacksThisTick = 0;
            return;
        }

        boolean isAttack = false;

        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            isAttack = true;
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            isAttack = packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK;
        }

        if (!isAttack) return;

        attacksThisTick++;

        long now = System.nanoTime();
        long deltaMs = (now - lastAttackNanos) / 1_000_000L;

        // Under min-attack-interval ms between attacks is physically impossible without cheats.
        long effectiveMinInterval = minAttackIntervalMs;
        if (ViaVersionUtil.isViaBackwardsPre1_9(player)) {
            effectiveMinInterval = (long)(minAttackIntervalMs / VIA_BACKWARDS_INTERVAL_MULTIPLIER);
        }

        if (lastAttackNanos > 0 && deltaMs < effectiveMinInterval) {
            rapidAttacks++;
            if (rapidAttacks > rapidAttacksThreshold) {
                flagAndAlert("delta=" + deltaMs + "ms rapid=" + rapidAttacks);
            }
        } else {
            rapidAttacks = Math.max(0, rapidAttacks - 1);
        }

        lastAttackNanos = now;
    }
}
