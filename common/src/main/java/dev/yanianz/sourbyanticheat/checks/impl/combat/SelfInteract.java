package dev.yanianz.sourbyanticheat.checks.impl.combat;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSpectateEntity;

@CheckData(name = "SelfInteract", stableKey = "sac.badpackets.self_hit", description = "Interacted with self", decay = 0.02)
public class SelfInteract extends Check implements PacketCheck {
    public SelfInteract(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            onInteract(event, packet.getEntityId());
        }

        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            WrapperPlayClientAttack packet = new WrapperPlayClientAttack(event);
            onInteract(event, packet.getEntityId());
        }

        if (event.getPacketType() == PacketType.Play.Client.SPECTATE_ENTITY) {
            WrapperPlayClientSpectateEntity packet = new WrapperPlayClientSpectateEntity(event);
            onInteract(event, packet.getEntityId());
        }
    }

    private void onInteract(PacketReceiveEvent event, int entityId) {
        if (!isOwnEntity(entityId)) {
            reward();
            return;
        }

        // Instant ban: the player interacted with the entity they are rendering as.
        boolean flagged = flagAndAlert();
        if (flagged && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }

    /**
     * Returns true if the interacted entity is the player itself or the entity their
     * camera is currently bound to — interacting with either is impossible legitimately.
     */
    private boolean isOwnEntity(int entityId) {
        if (entityId == player.entityID) return true;

        PacketEntity target = player.compensatedEntities.getEntity(entityId);
        if (target == null) return false;

        for (PacketEntity camera : player.cameraEntity.getPossibilities()) {
            if (camera == target) return true;
        }
        return false;
    }
}
