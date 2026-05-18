package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;

@CheckData(name = "BadPacketsB", stableKey = "sac.badpackets.ignored_rotation", description = "Ignored set rotation packet")
public class BadPacketsB extends Check implements PacketCheck {

    public BadPacketsB(final SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (isTransaction(event.getPacketType())) {
            player.pendingRotations.removeIf(data -> {
                if (player.getLastTransactionReceived() > data.getTransaction()) {
                    if (!data.isAccepted()) {
                        flagAndAlert();
                    } else {
                        reward();
                    }

                    return true;
                }

                return false;
            });
        }
    }
}
