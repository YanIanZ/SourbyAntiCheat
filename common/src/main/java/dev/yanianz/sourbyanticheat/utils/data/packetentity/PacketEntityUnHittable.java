package dev.yanianz.sourbyanticheat.utils.data.packetentity;

import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import java.util.UUID;

public class PacketEntityUnHittable extends PacketEntity {

    public PacketEntityUnHittable(SacPlayer player, UUID uuid, EntityType type, double x, double y, double z) {
        super(player, uuid, type, x, y, z);
    }

    @Override
    public boolean canHit() {
        return false;
    }
}
