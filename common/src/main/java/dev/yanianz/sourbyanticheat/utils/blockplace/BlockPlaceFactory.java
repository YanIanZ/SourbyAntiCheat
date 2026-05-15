package dev.yanianz.sourbyanticheat.utils.blockplace;

import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockPlace;

public interface BlockPlaceFactory {
    void applyBlockPlaceToWorld(SacPlayer player, BlockPlace place);
}
