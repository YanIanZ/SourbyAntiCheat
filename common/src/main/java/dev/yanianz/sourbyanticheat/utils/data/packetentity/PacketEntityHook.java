package dev.yanianz.sourbyanticheat.utils.data.packetentity;

import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;

import java.util.UUID;

public class PacketEntityHook extends PacketEntityUnHittable {
    public int owner;
    public int attached = -1;

    public PacketEntityHook(SacPlayer player, UUID uuid, EntityType type, double x, double y, double z, int owner) {
        super(player, uuid, type, x, y, z);
        this.owner = owner;
    }
}
