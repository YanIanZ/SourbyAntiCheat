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
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "NoSwing", stableKey = "sac.combat.noswing", description = "Detects attacks without arm animation", setback = 10, decay = 0.02)
public class NoSwingAttack extends Check implements PacketCheck {

    private boolean sentSwing = false;
    // Monotonic timestamp (nanoTime) of the last swing — immune to wall-clock/NTP jumps.
    private long lastSwingNanos = 0;
    private int buffer = 0;

    // Config-wired thresholds (defaults equal prior hardcoded values).
    // swingWindowNanos: how recent a swing must be to count as paired with an attack.
    private long swingWindowNanos = 50L * 1_000_000L;
    private int flagThreshold = 3;
    private int minFlag = 2;

    public NoSwingAttack(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.swingWindowNanos = config.getIntElse(base + "swing-window", 50) * 1_000_000L;
        this.flagThreshold = config.getIntElse(base + "flag-threshold", 3);
        this.minFlag = config.getIntElse(base + "min-flag", 2);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            sentSwing = true;
            lastSwingNanos = System.nanoTime();
        }

        boolean isAttack = false;
        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            isAttack = true;
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                isAttack = true;
            }
        }

        if (isAttack) {
            // Exemption check first — must run before any buffer/state mutation so an
            // exempt attack never moves the check's state.
            if (player.inVehicle() || player.gamemode == GameMode.SPECTATOR) return;

            // 1.8 and older clients may reorder the swing/attack packets, so a swing can
            // legitimately arrive after the attack within the same tick. Don't flag them.
            if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) return;

            long now = System.nanoTime();

            if (!sentSwing || (now - lastSwingNanos) > swingWindowNanos) {
                buffer++;
                if (buffer > flagThreshold) {
                    flagAndAlert("swingDelta=" + ((now - lastSwingNanos) / 1_000_000L) + "ms buffer=" + buffer);
                }
            } else {
                buffer = Math.max(0, buffer - 1);
                if (buffer < minFlag) reward();
            }

            // Consume the swing so it cannot be reused by a later attack this tick.
            sentSwing = false;
        }

        // Clear an unconsumed swing only on a tick-boundary flying packet — not on every
        // flying packet — so a swing that arrives before a same-tick movement update is
        // still available to pair with an attack in that tick.
        if (isTickPacket(event.getPacketType())) {
            sentSwing = false;
        }
    }
}
