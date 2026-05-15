package dev.yanianz.sourbyanticheat.checks.impl.movement;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "Step", stableKey = "sac.movement.step", description = "Detects step/vault hacks", setback = 10, decay = 0.02)
public class Step extends Check implements PacketCheck {

    private static final double MAX_STEP = 0.63;
    private int stepFlags = 0;

    public Step(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.canFly || player.isFlying || player.isGliding || player.inVehicle()) return;

        double deltaY = player.y - player.lastY;

        if (deltaY > MAX_STEP && deltaY < 5.0) {
            stepFlags++;
            if (stepFlags > 2) {
                flagAndAlert("dY=" + String.format("%.3f", deltaY) + " flags=" + stepFlags);
            }
        } else {
            stepFlags = Math.max(0, stepFlags - 1);
            if (stepFlags < 1) reward();
        }
    }
}
