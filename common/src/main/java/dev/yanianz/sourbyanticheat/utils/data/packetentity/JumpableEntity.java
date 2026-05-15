package dev.yanianz.sourbyanticheat.utils.data.packetentity;

import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.data.VectorData;

import java.util.Set;

public interface JumpableEntity {

    boolean isJumping();

    void setJumping(boolean jumping);

    float getJumpPower();

    void setJumpPower(float jumpPower);

    boolean canPlayerJump(SacPlayer player);

    boolean hasSaddle();

    void executeJump(SacPlayer player, Set<VectorData> possibleVectors);

}
