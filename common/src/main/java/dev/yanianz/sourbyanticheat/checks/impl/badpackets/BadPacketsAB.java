package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;

@CheckData(name = "BadPacketsAB", stableKey = "sac.badpackets.invalid_steer", description = "Detects invalid vehicle steer packets")
public class BadPacketsAB extends Check implements PacketCheck {

    public BadPacketsAB(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.STEER_VEHICLE) return;

        WrapperPlayClientSteerVehicle steer = new WrapperPlayClientSteerVehicle(event);
        float sideways = steer.getSideways();
        float forward = steer.getForward();

        // Steer inputs are normalised to [-1, 1] — anything outside is a malformed packet.
        if (Math.abs(sideways) > 1.0f || Math.abs(forward) > 1.0f) {
            flagAndAlert("out_of_range side=" + String.format("%.2f", sideways) + " fwd=" + String.format("%.2f", forward));
            return;
        }

        // Non-zero steer input while not in a vehicle is impossible.
        if (!player.inVehicle() && (sideways != 0 || forward != 0)) {
            flagAndAlert("not_in_vehicle side=" + String.format("%.2f", sideways) + " fwd=" + String.format("%.2f", forward));
            return;
        }

        reward();
    }
}
