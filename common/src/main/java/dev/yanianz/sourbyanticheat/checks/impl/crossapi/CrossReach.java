package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.data.packetentity.PacketEntity;

@CheckData(name = "CrossReach", configName = "crossreach", decay = 0.01, setback = 15, stableKey = "cross.reach")
public class CrossReach extends Check implements PacketCheck {

    private int buffer;
    private static final double REACH_MARGIN = 0.5;
    private static final double NETTY_RATE_THRESHOLD = 15.0;

    public CrossReach(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR
                || player.inVehicle()) return;

        WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
        if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        PacketEntity entity = player.compensatedEntities.entityMap.get(interact.getEntityId());
        if (entity == null || entity.isDead) return;

        double maxReach = player.compensatedEntities.self.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);

        double ex = entity.trackedServerPosition.getPos().getX();
        double ey = entity.trackedServerPosition.getPos().getY();
        double ez = entity.trackedServerPosition.getPos().getZ();
        double dist = Math.sqrt(Math.pow(player.x - ex, 2) + Math.pow(player.y - ey, 2) + Math.pow(player.z - ez, 2));

        boolean outOfReach = dist > maxReach + REACH_MARGIN;

        if (!outOfReach) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Reach");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 3) {
            flagAndAlert(String.format("dist=%.2f max=%.2f netty=%.1f/s spartan=%s",
                dist, maxReach, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
