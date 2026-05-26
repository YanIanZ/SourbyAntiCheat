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
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * KillAura target-switching check — flags hitting too many distinct entities
 * within a short rolling window (multi-aura). Distinct from {@code MultiAttack}
 * (which is per-tick); this spans a configurable window. Alert-only by default
 * (setback = -1); profile- and leniency-aware via {@link Check}.
 *
 * <p>Conservative defaults (more than 4 unique targets within 1s) so crowded but
 * legitimate team-fights do not flag on a single burst.
 */
@CheckData(name = "KillAuraB", stableKey = "sac.combat.killaura.switch",
        description = "Switches between many attack targets too quickly", setback = -1, decay = 0.02)
public class KillAuraB extends Check implements PacketCheck {

    private final Map<Integer, Long> recentTargets = new HashMap<>();
    private int buffer = 0;
    private long windowMs = 1000L;
    private int maxTargets = 4;
    private int flagThreshold = 2;

    public KillAuraB(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String b = getConfigName() + ".";
        windowMs = config.getIntElse(b + "window-ms", 1000);
        maxTargets = config.getIntElse(b + "max-targets", 4);
        flagThreshold = config.getIntElse(b + "flag-threshold", 2);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        int entityId;
        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            entityId = new WrapperPlayClientAttack(event).getEntityId();
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity p = new WrapperPlayClientInteractEntity(event);
            if (p.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;
            entityId = p.getEntityId();
        } else {
            return;
        }

        if (player.gamemode == GameMode.SPECTATOR || player.inVehicle()) return;

        long now = System.currentTimeMillis();
        recentTargets.put(entityId, now);
        recentTargets.values().removeIf(ts -> now - ts > windowMs);

        int distinct = recentTargets.size();
        if (distinct > maxTargets) {
            buffer++;
            if (buffer > flagThreshold) {
                flagAndAlert("targets=" + distinct + " in " + windowMs + "ms");
            }
        } else if (distinct <= 1) {
            buffer = Math.max(0, buffer - 1);
            if (buffer == 0) reward();
        }
    }
}
