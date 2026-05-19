package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.collisions.datatypes.SimpleCollisionBox;
import dev.yanianz.sourbyanticheat.utils.data.packetentity.PacketEntity;
import dev.yanianz.sourbyanticheat.utils.nmsutil.ReachUtils;

@CheckData(name = "CrossReach", configName = "crossreach", decay = 0.01, setback = 15, stableKey = "cross.reach")
public class CrossReach extends Check implements PacketCheck {

    private int buffer;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double reachMargin        = 0.5;
    private double nettyRateThreshold = 120.0;

    public CrossReach(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        reachMargin        = config.getDoubleElse(base + "reach-margin", 0.5);
        nettyRateThreshold = config.getDoubleElse(base + "netty-rate-threshold", 120.0);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.compensatedEntities.self.isDead) return;

        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR
                || player.inVehicle()) return;

        WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
        if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        PacketEntity entity = player.compensatedEntities.entityMap.get(interact.getEntityId());
        if (entity == null || entity.isDead) return;

        double maxReach = player.compensatedEntities.self.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);

        // Distance to the nearest point of the target's bounding box, not root-to-root —
        // root distance overestimates for tall/wide entities and produces false positives.
        SimpleCollisionBox targetBox = entity.getPossibleCollisionBoxes();
        double dist = ReachUtils.getMinReachToBox(player, targetBox);

        boolean outOfReach = dist > maxReach + reachMargin;

        if (!outOfReach) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > nettyRateThreshold;

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
