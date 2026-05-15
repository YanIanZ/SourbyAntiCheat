package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsAA", stableKey = "sac.badpackets.invalid_pitch", description = "Detects impossible pitch angles", setback = 5)
public class BadPacketsAA extends Check implements PacketCheck {

    public BadPacketsAA(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        WrapperPlayClientPlayerFlying packet = new WrapperPlayClientPlayerFlying(event);
        if (!packet.hasRotationChanged()) return;

        float pitch = packet.getLocation().getPitch();
        if (Math.abs(pitch) > 90.1f) {
            flagAndAlert("pitch=" + String.format("%.1f", pitch));
        }
    }
}
