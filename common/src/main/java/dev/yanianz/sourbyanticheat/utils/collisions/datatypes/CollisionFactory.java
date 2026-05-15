package dev.yanianz.sourbyanticheat.utils.collisions.datatypes;

import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;

public interface CollisionFactory {
    CollisionBox fetch(SacPlayer player, ClientVersion version, WrappedBlockState block, int x, int y, int z);
}
