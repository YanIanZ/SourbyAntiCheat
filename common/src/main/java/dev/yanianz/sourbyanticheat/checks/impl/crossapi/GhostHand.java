package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.data.packetentity.PacketEntity;

@CheckData(name = "GhostHand", configName = "ghosthand", decay = 0.02, setback = 10, stableKey = "cross.ghosthand")
public class GhostHand extends Check implements PacketCheck {

    private int buffer;
    private static final double NETTY_RATE_THRESHOLD = 15.0;

    public GhostHand(SacPlayer player) {
        super(player);
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
        if (dist < 0.1 || dist > 8.0) return;

        int steps = (int) Math.ceil(dist * 4);
        int wallBlocks = 0;
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            int bx = (int) Math.floor(eyeX + dx * t);
            int by = (int) Math.floor(eyeY + dy * t);
            int bz = (int) Math.floor(eyeZ + dz * t);

            com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState block =
                player.compensatedWorld.getBlock(bx, by, bz);
            if (block != null && block.getType().isSolid()) {
                wallBlocks++;
            }
        }

        if (wallBlocks < 3) {
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
}
