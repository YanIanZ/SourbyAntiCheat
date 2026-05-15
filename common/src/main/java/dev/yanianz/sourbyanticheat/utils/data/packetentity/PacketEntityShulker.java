package dev.yanianz.sourbyanticheat.utils.data.packetentity;

import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.world.BlockFace;

import java.util.UUID;

public class PacketEntityShulker extends PacketEntity {
    public BlockFace facing = BlockFace.DOWN;

    public PacketEntityShulker(SacPlayer player, UUID uuid, EntityType type, double x, double y, double z) {
        super(player, uuid, type, x, y, z);
    }

    @Override
    public double clampScale(double scale) {
        return Math.min(scale, 3);
    }
}
