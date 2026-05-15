package dev.yanianz.sourbyanticheat.utils.collisions.datatypes;

import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;

public interface HitBoxFactory {
    CollisionBox fetch(SacPlayer player, StateType heldItem, ClientVersion version, WrappedBlockState block, boolean isTargetBlock, int x, int y, int z);
}
