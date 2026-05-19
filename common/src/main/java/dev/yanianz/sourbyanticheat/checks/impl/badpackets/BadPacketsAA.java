package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsAA", stableKey = "sac.badpackets.invalid_pitch", description = "Detects impossible pitch angles", setback = 5)
public class BadPacketsAA extends Check implements PacketCheck {

    // Vanilla clamps pitch to [-90, 90]; the 0.1 tolerance absorbs float noise.
    private static final float MAX_PITCH = 90.1f;

    public BadPacketsAA(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        WrapperPlayClientPlayerFlying packet = new WrapperPlayClientPlayerFlying(event);
        if (!packet.hasRotationChanged()) return;

        float pitch = packet.getLocation().getPitch();
        if (Math.abs(pitch) > MAX_PITCH) {
            flagAndAlert("pitch=" + String.format("%.1f", pitch));
        } else {
            reward();
        }
    }
}
