package dev.yanianz.sourbyanticheat.predictionengine.blockeffects;

import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.math.Vector3dm;

import java.util.List;

public interface BlockEffectsResolver {

    void applyEffectsFromBlocks(SacPlayer player, Vector3dm clientVelocity, boolean onlyApplyVelocity, List<SacPlayer.Movement> movements);

}
