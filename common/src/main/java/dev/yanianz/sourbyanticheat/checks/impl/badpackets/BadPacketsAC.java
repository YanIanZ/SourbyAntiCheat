package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;

@CheckData(name = "BadPacketsAC", stableKey = "sac.badpackets.invalid_transaction", description = "Detects spoofed transaction/pong packets", setback = 10)
public class BadPacketsAC extends Check implements PacketCheck {

    private int lastTransactionId = -1;
    private int spoofCount = 0;

    public BadPacketsAC(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PONG
            && event.getPacketType() != PacketType.Play.Client.WINDOW_CONFIRMATION) return;

        int id;
        if (event.getPacketType() == PacketType.Play.Client.PONG) {
            id = new WrapperPlayClientPong(event).getId();
        } else {
            id = new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation(event).getActionId();
        }

        if (lastTransactionId == -1) { lastTransactionId = id; return; }

        if (id < lastTransactionId) {
            spoofCount++;
            if (spoofCount > 3) {
                flagAndAlert("seq=" + id + " last=" + lastTransactionId + " spoofs=" + spoofCount);
            }
        } else {
            spoofCount = Math.max(0, spoofCount - 1);
            reward();
        }
        lastTransactionId = id;
    }
}
