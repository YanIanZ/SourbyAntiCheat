package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import dev.yanianz.sourbyanticheat.utils.data.packetentity.PacketEntity;

@CheckData(name = "FightBot", configName = "fightbot", decay = 0.01, setback = 15, stableKey = "cross.fightbot")
public class FightBot extends Check implements PostPredictionCheck {

    private int buffer;
    private int perfectAimStreak;
    private int attackedEntity = -1;

    public FightBot(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;
        WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
        if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            attackedEntity = interact.getEntityId();
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        if (attackedEntity < 0) {
            perfectAimStreak = Math.max(0, perfectAimStreak - 1);
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR
                || player.inVehicle() || player.compensatedEntities.self.isDead) {
            attackedEntity = -1;
            return;
        }

        PacketEntity entity = player.compensatedEntities.entityMap.get(attackedEntity);
        attackedEntity = -1;
        if (entity == null || entity.isDead) return;

        double ex = entity.trackedServerPosition.getPos().getX();
        double ez = entity.trackedServerPosition.getPos().getZ();
        double angleToEntity = Math.toDegrees(Math.atan2(ex - player.x, ez - player.z));
        double yawDelta = Math.abs(player.yaw - angleToEntity);
        double normalizedDelta = yawDelta > 180 ? 360 - yawDelta : yawDelta;

        if (normalizedDelta < 2.0 && Math.abs(player.crossValidationData.peRotationDeltaYaw) > 5.0) {
            perfectAimStreak++;
        } else {
            perfectAimStreak = Math.max(0, perfectAimStreak - 1);
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        if (perfectAimStreak < 5) return;

        boolean nettyConfirms = player.crossValidationData.nettyIntervalVariance < 15.0;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "KillAura");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 4) {
            flagAndAlert(String.format("yawErr=%.1f streak=%d nettyVar=%.1f spartan=%s",
                normalizedDelta, perfectAimStreak, player.crossValidationData.nettyIntervalVariance, spartanResult.type()));
        }
    }
}
