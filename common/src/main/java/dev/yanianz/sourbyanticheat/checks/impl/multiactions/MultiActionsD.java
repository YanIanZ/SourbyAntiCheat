package dev.yanianz.sourbyanticheat.checks.impl.multiactions;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

@CheckData(name = "MultiActionsD", stableKey = "sac.multiactions.inventory_close_while_moving", description = "Closed inventory while moving")
public class MultiActionsD extends Check implements PacketCheck {
    public MultiActionsD(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLOSE_WINDOW) return;
        if (player.serverOpenedInventoryThisTick) return;

        String verbose = MultiActionsC.getVerbose(player);
        if (verbose.isEmpty()) return;

        // Don't cancel this packet, because it won't do anything except for making chests
        // look like they are still open (desynced),
        // and it can cause incompatibility issues with plugins
        flagAndAlert(verbose);
    }
}
