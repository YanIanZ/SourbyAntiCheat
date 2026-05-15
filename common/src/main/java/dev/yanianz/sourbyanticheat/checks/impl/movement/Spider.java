package dev.yanianz.sourbyanticheat.checks.impl.movement;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "Spider", stableKey = "sac.movement.spider", description = "Detects spider/wall climb hacks", setback = 10, decay = 0.02)
public class Spider extends Check implements PostPredictionCheck {

    private int climbTicks = 0;
    private double lastDeltaY = 0;
    private boolean wasOnGround = true;

    public Spider(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.canFly || player.isFlying || player.isGliding || player.inVehicle()) {
            climbTicks = 0;
            return;
        }

        WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
        double deltaY = player.y - player.lastY;

        if (deltaY < 0.01 && deltaY > -0.01) {
            climbTicks = Math.max(0, climbTicks - 1);
            return;
        }

        if (flying.isOnGround()) {
            climbTicks = 0;
            wasOnGround = true;
            reward();
            return;
        }

        if (deltaY > 0.1 && !wasOnGround && climbTicks > 3) {
            climbTicks++;
            if (climbTicks > 8) {
                flagAndAlert("dY=" + String.format("%.3f", deltaY) + " ticks=" + climbTicks);
            }
        } else if (deltaY > 0) {
            climbTicks++;
        } else {
            climbTicks = Math.max(0, climbTicks - 2);
            if (climbTicks < 1) reward();
        }

        lastDeltaY = deltaY;
        if (flying.isOnGround()) wasOnGround = true;
        else if (!wasOnGround && deltaY < -0.01) wasOnGround = false;
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
    }
}
