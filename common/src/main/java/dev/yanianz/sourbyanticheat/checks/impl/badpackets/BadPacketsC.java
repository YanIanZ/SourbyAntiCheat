package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "BadPacketsC", stableKey = "sac.badpackets.wake_not_sleeping", description = "Tried to wake up while not sleeping")
public class BadPacketsC extends Check implements PacketCheck {
    public BadPacketsC(@NotNull SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION
                && new WrapperPlayClientEntityAction(event).getAction() == WrapperPlayClientEntityAction.Action.LEAVE_BED) {
            if (player.isInBed || player.lastInBed) {
                // lastInBed exemption: the bed may have been destroyed this tick, so the
                // client legitimately sends LEAVE_BED a tick after the server-side state cleared
                reward();
            } else {
                flagAndAlert();
            }
        }
    }
}
