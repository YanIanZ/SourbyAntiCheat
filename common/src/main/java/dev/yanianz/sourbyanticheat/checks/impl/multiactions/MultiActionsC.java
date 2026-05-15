package dev.yanianz.sourbyanticheat.checks.impl.multiactions;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;

@CheckData(name = "MultiActionsC", stableKey = "sac.multiactions.inventory_click_while_moving", description = "Clicked in inventory while moving")
public class MultiActionsC extends Check implements PacketCheck {
    public MultiActionsC(SacPlayer player) {
        super(player);
    }

    // TODO: move this to a better spot? not sure where to put this
    @Contract(pure = true)
    public static String getVerbose(@NotNull SacPlayer player) {
        StringJoiner verbose = new StringJoiner(", ");
        if (player.isSprinting && (!player.isSwimming || !player.clientClaimsLastOnGround)) {
            verbose.add("sprinting");
        }

        if (player.isSneaking && player.getClientVersion().isOlderThan(ClientVersion.V_1_15)) {
            verbose.add("sneaking");
        }

        if (player.supportsEndTick() && player.packetStateData.knownInput.moving()) {
            verbose.add("input");
        }

        return verbose.toString();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) return;
        if (player.serverOpenedInventoryThisTick) return;

        String verbose = getVerbose(player);
        if (verbose.isEmpty()) return;

        if (flagAndAlert(verbose) && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }
}
