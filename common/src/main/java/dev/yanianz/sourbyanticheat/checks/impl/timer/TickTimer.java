package dev.yanianz.sourbyanticheat.checks.impl.timer;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

import static com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying;

@CheckData(name = "TickTimer", stableKey = "sac.timer.tick", setback = 1)
public class TickTimer extends Check implements PacketCheck {

    private boolean receivedTickEnd = true;
    private int flyingPackets = 0;

    public TickTimer(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!player.supportsEndTick()) return;
        if (isFlying(event.getPacketType()) && !player.packetStateData.lastPacketWasTeleport) {
            if (!receivedTickEnd && flagAndAlertWithSetback("type=flying, packets=" + flyingPackets)) {
                handleViolation();
            }
            receivedTickEnd = false;
            flyingPackets++;
        } else if (event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END) {
            receivedTickEnd = true;
            if (flyingPackets > 1 && flagAndAlertWithSetback("type=end, packets=" + flyingPackets)) {
                handleViolation();
            }
            flyingPackets = 0;
        }
    }

    private void handleViolation() {
        // Although we don't cancel the packet, this should be counted as an invalid packet.
        player.onPacketCancel();
    }
}
