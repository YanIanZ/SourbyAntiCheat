package dev.yanianz.sourbyanticheat.checks.impl.sprint;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

@CheckData(name = "SprintE", stableKey = "sac.sprint.wall", description = "Sprinting while colliding with a wall", setback = 5)
public class SprintE extends Check implements PostPredictionCheck {
    private boolean startedSprintingThisTick, wasHardHorizontalCollision;

    public SprintE(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            if (new WrapperPlayClientEntityAction(event).getAction() == WrapperPlayClientEntityAction.Action.START_SPRINTING) {
                startedSprintingThisTick = true;
            }
        }
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked()) return;

        if (wasHardHorizontalCollision && !startedSprintingThisTick && !player.uncertaintyHandler.isNearGlitchyBlock
                && !player.inVehicle() && !player.uncertaintyHandler.lastVehicleSwitch.hasOccurredSince(0)
                && (!player.wasTouchingWater || player.getClientVersion().isOlderThan(ClientVersion.V_1_13))
                && player.wasLastPredictionCompleteChecked) {
            if (player.isSprinting) {
                flagAndAlertWithSetback();
            } else {
                reward();
            }
        }

        wasHardHorizontalCollision = player.horizontalCollision && !player.softHorizontalCollision && player.wasLastPredictionCompleteChecked;
        startedSprintingThisTick = false;
    }
}
