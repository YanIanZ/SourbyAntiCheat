package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerAbilities;

@CheckData(name = "BadPacketsI", stableKey = "sac.badpackets.spoofed_abilities", description = "Claimed to be flying while unable to fly")
public class BadPacketsI extends Check implements PacketCheck {
    public BadPacketsI(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ABILITIES) {
            if (new WrapperPlayClientPlayerAbilities(event).isFlying() && !player.canFly
                    // gliding clients can toggle the flying ability bit during the
                    // elytra transition before the server-side canFly state updates
                    && !player.isGliding) {
                if (flagAndAlert() && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            } else {
                reward();
            }
        }
    }
}
