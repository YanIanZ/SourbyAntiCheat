package dev.yanianz.sourbyanticheat.checks.impl.packetorder;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

import java.util.ArrayDeque;

@CheckData(name = "PacketOrderG", stableKey = "sac.packetorder.hotbar_inventory_manage_order", experimental = true)
public class PacketOrderG extends Check implements PostPredictionCheck {
    public PacketOrderG(SacPlayer player) {
        super(player);
    }

    private final ArrayDeque<String> flags = new ArrayDeque<>();

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING || (event.getPacketType() == PacketType.Play.Client.CLIENT_STATUS
                && new WrapperPlayClientClientStatus(event).getAction() == WrapperPlayClientClientStatus.Action.OPEN_INVENTORY_ACHIEVEMENT)) {
            DiggingAction action = null;
            if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
                action = new WrapperPlayClientPlayerDigging(event).getAction();
                if (action != DiggingAction.SWAP_ITEM_WITH_OFFHAND
                        && action != DiggingAction.DROP_ITEM
                        && action != DiggingAction.DROP_ITEM_STACK
                ) return;
            }

            if (player.packetOrderProcessor.isAttackingOrStabbing()
                    || player.packetOrderProcessor.isReleasing()
                    || player.packetOrderProcessor.isRightClicking()
                    || player.packetOrderProcessor.isPicking()
                    || player.packetOrderProcessor.isDigging()
            ) {
                String verbose = "action=" + (action == null ? "openInventory" : action == DiggingAction.SWAP_ITEM_WITH_OFFHAND ? "swap" : "drop")
                        + ", attacking=" + player.packetOrderProcessor.isAttackingOrStabbing()
                        + ", releasing=" + player.packetOrderProcessor.isReleasing()
                        + ", rightClicking=" + player.packetOrderProcessor.isRightClicking()
                        + ", picking=" + player.packetOrderProcessor.isPicking()
                        + ", digging=" + player.packetOrderProcessor.isDigging();
                if (!player.canSkipTicks()) {
                    if (flagAndAlert(verbose) && shouldModifyPackets() && canCancel(action)) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    flags.add(verbose);
                }
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (String verbose : flags) {
                flagAndAlert(verbose);
            }
        }

        flags.clear();
    }
}
