package dev.yanianz.sourbyanticheat.checks.impl.packetorder;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;

@CheckData(name = "PacketOrderM", stableKey = "sac.packetorder.interact_use_order", experimental = true)
public class PacketOrderM extends Check implements PostPredictionCheck {
    public PacketOrderM(final SacPlayer player) {
        super(player);
    }

    private int invalid;
    private boolean usingWithoutInteract, interacting;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            if (new WrapperPlayClientInteractEntity(event).getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                interacting = true;
                if (usingWithoutInteract) {
                    if (!player.canSkipTicks()) {
                        if (flagAndAlert() && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                        }
                    } else {
                        invalid++;
                    }
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM
                || event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT
                && new WrapperPlayClientPlayerBlockPlacement(event).getFace() == BlockFace.OTHER) {
            if (!interacting) {
                usingWithoutInteract = true;
            }

            interacting = false;
        }

        if (!player.cameraEntity.isSelf() || isTickPacket(event.getPacketType())) {
            usingWithoutInteract = interacting = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (; invalid >= 1; invalid--) {
                flagAndAlert();
            }
        }

        invalid = 0;
    }
}
