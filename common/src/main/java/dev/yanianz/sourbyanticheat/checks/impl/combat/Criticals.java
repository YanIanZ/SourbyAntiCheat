package dev.yanianz.sourbyanticheat.checks.impl.combat;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "Criticals", stableKey = "sac.combat.criticals", description = "Detects critical hit manipulation", setback = 5, decay = 0.02)
public class Criticals extends Check implements PacketCheck {

    private double lastDeltaY = 0;
    private int critFlags = 0;

    public Criticals(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

        WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
        if (packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        double deltaY = player.y - player.lastY;
        boolean wasFalling = lastDeltaY < -0.01;

        if (deltaY > -0.01 && wasFalling && !player.isGliding && !player.inVehicle()) {
            critFlags++;
            if (critFlags > 5) {
                flagAndAlert("dY=" + String.format("%.3f", deltaY) + " flags=" + critFlags);
            }
        } else {
            critFlags = Math.max(0, critFlags - 1);
            if (critFlags < 2) reward();
        }

        lastDeltaY = deltaY;
    }
}
