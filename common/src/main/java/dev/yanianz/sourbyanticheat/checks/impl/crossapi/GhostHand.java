package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.collisions.CollisionData;
import dev.yanianz.sourbyanticheat.utils.collisions.datatypes.SimpleCollisionBox;
import dev.yanianz.sourbyanticheat.utils.data.packetentity.PacketEntity;
import dev.yanianz.sourbyanticheat.utils.math.Vector3dm;
import dev.yanianz.sourbyanticheat.utils.nmsutil.Ray;

import java.util.ArrayList;
import java.util.List;

@CheckData(name = "GhostHand", configName = "ghosthand", decay = 0.02, setback = 10, stableKey = "cross.ghosthand")
public class GhostHand extends Check implements PacketCheck {

    private int buffer;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private int wallBlocksThreshold = 3;
    private double maxAttackDistance = 8.0;
    private static final double NETTY_RATE_THRESHOLD = 120.0;

    public GhostHand(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        wallBlocksThreshold = config.getIntElse(base + "wall-blocks-threshold", 3);
        maxAttackDistance   = config.getDoubleElse(base + "max-attack-distance", 8.0);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
        if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        PacketEntity entity = player.compensatedEntities.entityMap.get(interact.getEntityId());
        if (entity == null || entity.isDead) return;

        double eyeX = player.x;
        double eyeY = player.y + 1.62;
        double eyeZ = player.z;
        double targetX = entity.trackedServerPosition.getPos().getX();
        double targetY = entity.trackedServerPosition.getPos().getY();
        double targetZ = entity.trackedServerPosition.getPos().getZ();

        double dx = targetX - eyeX;
        double dy = targetY - eyeY;
        double dz = targetZ - eyeZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.1 || dist > maxAttackDistance) return;

        // Proper AABB intersection: cast the eye->target segment against each block's
        // actual collision box (handles thin / diagonal / partial-block geometry that
        // block-center sampling misses).
        Ray ray = new Ray(new Vector3dm(eyeX, eyeY, eyeZ),
            new Vector3dm(dx / dist, dy / dist, dz / dist));
        int wallBlocks = countBlockedBlocks(ray, eyeX, eyeY, eyeZ, dx, dy, dz, dist);

        if (wallBlocks < wallBlocksThreshold) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "KillAura");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            buffer += 2;
        } else {
            buffer += 1;
        }

        if (buffer > 3) {
            flagAndAlert(String.format("wallBlocks=%d dist=%.1f netty=%.1f/s spartan=%s",
                wallBlocks, dist,
                player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }

    private int countBlockedBlocks(Ray ray, double eyeX, double eyeY, double eyeZ,
                                   double dx, double dy, double dz, double dist) {
        // Iterate the unique blocks the segment passes through at a fine step, then test
        // the ray against each block's real collision box exactly once per block.
        int steps = (int) Math.ceil(dist * 4);
        int wallBlocks = 0;
        long lastKey = Long.MIN_VALUE;
        List<SimpleCollisionBox> boxes = new ArrayList<>();
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int bx = (int) Math.floor(eyeX + dx * t);
            int by = (int) Math.floor(eyeY + dy * t);
            int bz = (int) Math.floor(eyeZ + dz * t);

            long key = (((long) bx & 0x3FFFFF) << 42) | (((long) by & 0xFFFFF) << 22) | ((long) bz & 0x3FFFFF);
            if (key == lastKey) continue;
            lastKey = key;

            com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState block =
                player.compensatedWorld.getBlock(bx, by, bz);
            if (block == null || !block.getType().isSolid()) continue;

            boxes.clear();
            CollisionData.getData(block.getType())
                .getMovementCollisionBox(player, player.getClientVersion(), block, bx, by, bz)
                .downCast(boxes);

            for (SimpleCollisionBox box : boxes) {
                if (box.intersectsRay(ray, 0f, (float) dist) != null) {
                    wallBlocks++;
                    break;
                }
            }
        }
        return wallBlocks;
    }
}
