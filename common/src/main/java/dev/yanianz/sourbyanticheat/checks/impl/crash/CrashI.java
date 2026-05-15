package dev.yanianz.sourbyanticheat.checks.impl.crash;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSelectBundleItem;

@CheckData(name = "CrashI", stableKey = "sac.crash.invalid_bundle_slot")
public class CrashI extends Check implements PacketCheck {
    public CrashI(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.SELECT_BUNDLE_ITEM) {
            int selectedItemIndex;
            try {
                selectedItemIndex = new WrapperPlayClientSelectBundleItem(event).getSelectedItemIndex();
            } catch (IllegalArgumentException e) {
                // thanks packetevents!
                if (e.getMessage().startsWith("Invalid selectedItemIndex: ")) {
                    selectedItemIndex = Integer.parseInt(e.getMessage().substring(27));
                } else {
                    throw e;
                }
            }

            if (selectedItemIndex < -1) {
                flagAndAlert("selectedItemIndex=" + selectedItemIndex);
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }
    }
}
