package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;

@CheckData(name = "BadPacketsAC", stableKey = "sac.badpackets.invalid_transaction", description = "Detects spoofed transaction/pong packets", setback = 10, decay = 0.01)
public class BadPacketsAC extends Check implements PacketCheck {

    private int lastPongId = -1;
    private int lastWindowId = -1;
    private int spoofCount = 0;

    public BadPacketsAC(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        int id;
        boolean isPong;

        if (event.getPacketType() == PacketType.Play.Client.PONG) {
            id = new WrapperPlayClientPong(event).getId();
            isPong = true;
        } else if (event.getPacketType() == PacketType.Play.Client.WINDOW_CONFIRMATION) {
            id = new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation(event).getActionId();
            isPong = false;
        } else {
            return;
        }

        int lastId = isPong ? lastPongId : lastWindowId;
        if (lastId == -1) {
            if (isPong) lastPongId = id; else lastWindowId = id;
            return;
        }

        boolean backward = id < lastId && (lastId - id) > 5000;
        boolean forwardSpoof = id > lastId && (id - lastId) > 5000;
        if (backward || forwardSpoof) {
            int diff = backward ? (lastId - id) : (id - lastId);
            spoofCount++;
            if (spoofCount > 10) {
                flagAndAlert("type=" + (isPong ? "pong" : "window") + " id=" + id + " last=" + lastId + " diff=" + diff + " dir=" + (backward ? "backward" : "forward"));
            }
        } else {
            spoofCount = Math.max(0, spoofCount - 1);
            reward();
        }

        if (isPong) lastPongId = id; else lastWindowId = id;
    }
}
