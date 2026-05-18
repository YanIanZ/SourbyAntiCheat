package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "BadPacketsAE", stableKey = "sac.badpackets.invalid_entity_interact", description = "Detects interacting with non-existent entities", setback = 5, decay = 0.01)
public class BadPacketsAE extends Check implements PacketCheck {

    private int invalidCount = 0;

    public BadPacketsAE(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;
        if (player.inVehicle()) return;

        int entityId = new WrapperPlayClientInteractEntity(event).getEntityId();

        // A legitimate interaction targets a real, lag-compensated-tracked entity (or the
        // player themselves — self-interact is handled by the SelfInteract check). Only an
        // interaction with an entity the tracker has no record of is the cheat signal.
        // The previous logic counted EVERY entity interaction and never decremented, so a
        // normal player flagged forever once they had interacted with 20+ entities.
        if (entityId == player.entityID || player.compensatedEntities.getEntity(entityId) != null) {
            invalidCount = Math.max(0, invalidCount - 1);
            reward();
            return;
        }

        invalidCount++;
        if (invalidCount > 20) {
            flagAndAlert("invalid_interacts=" + invalidCount);
            invalidCount = 0; // reset after a flag so it does not alert on every later packet
            return;
        }
        reward();
    }
}
