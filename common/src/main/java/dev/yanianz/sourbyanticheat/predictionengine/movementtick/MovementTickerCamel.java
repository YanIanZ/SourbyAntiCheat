package dev.yanianz.sourbyanticheat.predictionengine.movementtick;

import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.data.SprintingState;
import dev.yanianz.sourbyanticheat.utils.data.packetentity.PacketEntityCamel;

public class MovementTickerCamel extends MovementTickerHorse {

    public MovementTickerCamel(SacPlayer player) {
        super(player);
    }

    @Override
    public float getExtraSpeed() {
        PacketEntityCamel camel = (PacketEntityCamel) player.compensatedEntities.self.getRiding();

        // If jumping... speed wouldn't apply after this
        // This engine was not designed for this edge case
        final boolean wantsToJump = camel.getJumpPower() > 0.0F && !camel.isJumping() && player.lastOnGround;
        if (wantsToJump) return 0;

        return player.vehicleData.camelSprintingState != SprintingState.STOPPED && camel.getDashCooldown() <= 0 && !camel.isDashing() ? 0.1f : 0.0f;
    }
}
