package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossCriticals", configName = "crosscriticals", decay = 0.02, setback = 10, stableKey = "cross.criticals")
public class CrossCriticals extends Check implements PostPredictionCheck {

    private int buffer;
    private boolean attackedThisTick = false;
    private static final double NETTY_RATE_THRESHOLD = 15.0;

    public CrossCriticals(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            attackedThisTick = interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        if (!attackedThisTick) {
            buffer = Math.max(0, buffer - 1);
            reward();
            // dead code removed: attackedThisTick = false was unreachable here (already returned)
            return;
        }
        attackedThisTick = false;

        if (player.inVehicle() || player.isGliding || player.canFly
                || player.wasTouchingWater || player.compensatedEntities.self.isDead
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.LEVITATION)
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.SLOW_FALLING)
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        // Wind-burst exemption — explosion-type velocity (incl. wind charges in 1.21+) can launch a player
        // upward; criticals check would falsely fire since the player is airborne and moving up.
        if (player.firstBreadExplosion != null || player.likelyExplosions != null) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        double deltaY = player.crossValidationData.pePositionDeltaY;
        boolean notFalling = deltaY > -0.01;

        if (!notFalling) {
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Criticals");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 4) {
            flagAndAlert(String.format("dY=%.3f netty=%.1f/s spartan=%s",
                deltaY, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
