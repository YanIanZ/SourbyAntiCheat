package dev.yanianz.sourbyanticheat.checks.impl.elytra;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

@CheckData(name = "ElytraB", stableKey = "sac.elytra.no_jump", description = "Started gliding without jumping")
public class ElytraB extends Check implements PostPredictionCheck {
    private boolean glide;
    private boolean setback;

    public ElytraB(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION
                && new WrapperPlayClientEntityAction(event).getAction() == WrapperPlayClientEntityAction.Action.START_FLYING_WITH_ELYTRA
                && player.supportsEndTick()
        ) {
            if (player.packetStateData.knownInput.jump()) {
                if (flagAndAlert("no release")) {
                    setback = true;
                    if (shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                        player.resyncPose();
                    }
                }
            } else {
                glide = true;
            }
        }

        if (isUpdate(event.getPacketType())) {
            if (glide && !player.packetStateData.knownInput.jump() && flagAndAlert("no jump")) {
                setback = true;
            }

            glide = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (setback) {
            setback = false;
            setbackIfAboveSetbackVL();
        }
    }
}
