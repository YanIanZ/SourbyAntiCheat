package dev.yanianz.sourbyanticheat.checks.impl.sprint;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

import static com.github.retrooper.packetevents.protocol.potion.PotionTypes.BLINDNESS;

@CheckData(name = "SprintD", stableKey = "sac.sprint.blindness", description = "Started sprinting while having blindness", setback = 5, experimental = true)
public class SprintD extends Check implements PostPredictionCheck {
    public boolean startedSprintingBeforeBlind = false;

    public SprintD(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            if (new WrapperPlayClientEntityAction(event).getAction() == WrapperPlayClientEntityAction.Action.START_SPRINTING) {
                startedSprintingBeforeBlind = false;
            }
        }
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (player.compensatedEntities.self.hasPotionEffect(BLINDNESS) && !startedSprintingBeforeBlind) {
            if (player.isSprinting) {
                flagAndAlertWithSetback();
            } else reward();
        }
    }
}
