package dev.yanianz.sourbyanticheat.checks.impl.movement;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "Speed", stableKey = "sac.movement.speed", description = "Detects horizontal speed hacks", setback = 10, decay = 0.01)
public class Speed extends Check implements PostPredictionCheck {

    private static final double BASE_WALK_SPEED = 0.217;
    private static final double BASE_SPRINT_SPEED = 0.33;
    private static final double MAX_EFFECT_SPEED = 0.60;
    private static final double BUFFER_DECAY = 0.01;
    private static final double SPEED_PER_LEVEL = 0.06;

    private double buffer = 0;

    public Speed(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate) return;

        double deltaX = player.x - player.lastX;
        double deltaZ = player.z - player.lastZ;
        double deltaH = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (deltaH < 0.001) {
            buffer = Math.max(0, buffer - BUFFER_DECAY);
            return;
        }

        int speedLevel = player.compensatedEntities.self.getPotionEffectLevel(PotionTypes.SPEED).orElse(-1);
        double speedMultiplier = speedLevel >= 0 ? 1.0 + (speedLevel + 1) * 0.2 : 1.0;

        double maxSpeed;
        if (player.isFlying || player.canFly) {
            maxSpeed = MAX_EFFECT_SPEED;
        } else {
            maxSpeed = BASE_SPRINT_SPEED * speedMultiplier;
            if (!player.isSprinting && deltaH <= maxSpeed * 0.75) {
                maxSpeed = BASE_WALK_SPEED * speedMultiplier;
            }
        }

        double excess = deltaH - maxSpeed;
        if (excess > 0.01) {
            buffer += excess;
            if (buffer > 2.5) {
                flagAndAlert("h=" + String.format("%.3f", deltaH) + " max=" + String.format("%.3f", maxSpeed) + " buffer=" + String.format("%.3f", buffer));
            }
        } else {
            buffer = Math.max(0, buffer - BUFFER_DECAY);
            reward();
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
    }
}
