package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsAF", stableKey = "sac.badpackets.ground_spoof_flying", description = "Detects ground spoof mismatch in flying packets", setback = 10)
public class BadPacketsAF extends Check implements PacketCheck {

    public BadPacketsAF(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport
            || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate) return;
        if (player.isFlying || player.canFly || player.isGliding || player.inVehicle()) return;

        WrapperPlayClientPlayerFlying packet = new WrapperPlayClientPlayerFlying(event);
        if (!packet.hasPositionChanged()) return;

        double deltaY = player.y - player.lastY;
        boolean claimedGround = packet.isOnGround();

        if (claimedGround && Math.abs(deltaY) > 0.05 && deltaY < 0 && !player.lastOnGround) {
            flagAndAlert("dY=" + String.format("%.3f", deltaY) + " claimedGround=true");
        }
    }
}
