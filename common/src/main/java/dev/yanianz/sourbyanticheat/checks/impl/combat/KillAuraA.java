// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.collisions.datatypes.SimpleCollisionBox;
import dev.yanianz.sourbyanticheat.utils.data.packetentity.PacketEntity;
import dev.yanianz.sourbyanticheat.utils.math.Vector3dm;
import dev.yanianz.sourbyanticheat.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

/**
 * KillAura angle check — flags attacking a living entity whose centre lies far
 * outside the player's look direction (e.g. behind or beside them). Alert-only
 * by default (setback = -1); profile- and leniency-aware via {@link Check}.
 *
 * <p>Deliberately conservative: only mid-range hits (between {@code min-distance}
 * and {@code max-distance}) are scored, with a wide default angle tolerance and a
 * buffer, so normal close-quarters combat does not flag.
 */
@CheckData(name = "KillAuraA", stableKey = "sac.combat.killaura.angle",
        description = "Attacks an entity outside a plausible look angle", setback = -1, decay = 0.02)
public class KillAuraA extends Check implements PacketCheck {

    private int buffer = 0;
    private double maxAngle = 80.0;
    private double minDistance = 1.5;
    private double maxDistance = 6.0;
    private int flagThreshold = 4;
    private int minFlag = 2;

    public KillAuraA(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String b = getConfigName() + ".";
        maxAngle = config.getDoubleElse(b + "max-angle", 80.0);
        minDistance = config.getDoubleElse(b + "min-distance", 1.5);
        maxDistance = config.getDoubleElse(b + "max-distance", 6.0);
        flagThreshold = config.getIntElse(b + "flag-threshold", 4);
        minFlag = config.getIntElse(b + "min-flag", 2);
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
        if (player.packetStateData.lastPacketWasTeleport) return;

        PacketEntity entity = player.compensatedEntities.entityMap.get(entityId);
        if (entity == null || entity.isDead || !entity.isLivingEntity || entity.riding != null) return;

        SimpleCollisionBox box = entity.getPossibleCollisionBoxes();
        double cx = (box.minX + box.maxX) / 2.0;
        double cy = (box.minY + box.maxY) / 2.0;
        double cz = (box.minZ + box.maxZ) / 2.0;

        double[] eyeHeights = player.getPossibleEyeHeights();
        double eyeH = eyeHeights.length > 0 ? eyeHeights[eyeHeights.length - 1] : 1.62;
        double dx = cx - player.x;
        double dy = cy - (player.y + eyeH);
        double dz = cz - player.z;

        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < minDistance || dist > maxDistance) return; // point-blank + far hits are unreliable

        Vector3dm look = ReachUtils.getLook(player, player.yaw, player.pitch);
        double angle = KillAuraMath.angleDegrees(look.getX(), look.getY(), look.getZ(), dx, dy, dz);

        if (angle > maxAngle) {
            buffer++;
            if (buffer > flagThreshold) {
                flagAndAlert(String.format("angle=%.1f dist=%.2f", angle, dist));
            }
        } else {
            buffer = Math.max(0, buffer - 1);
            if (buffer < minFlag) reward();
        }
    }
}
