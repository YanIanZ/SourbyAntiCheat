package dev.yanianz.sourbyanticheat.checks.impl.packetorder;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

import static com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying;

@CheckData(name = "PacketOrderO", stableKey = "sac.packetorder.tick_end_order", experimental = true)
public class PacketOrderO extends Check implements PacketCheck {
    public PacketOrderO(final SacPlayer player) {
        super(player);
    }

    private boolean flying;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END) {
            flying = false;
        }

        if (isFlying(event.getPacketType()) && player.supportsEndTick() && !player.packetStateData.lastPacketWasTeleport) {
            flying = true;
            return;
        }

        if (flying && !isAsync(event.getPacketType()) && event.getPacketType() != PacketType.Play.Client.VEHICLE_MOVE) {
            if (player.inVehicle() && event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
                WrapperPlayClientEntityAction.Action action = new WrapperPlayClientEntityAction(event).getAction();
                if (action == WrapperPlayClientEntityAction.Action.START_SPRINTING || action == WrapperPlayClientEntityAction.Action.STOP_SPRINTING) {
                    return;
                }
            }

            flagAndAlert("type=" + event.getPacketType());
        }
    }
}
