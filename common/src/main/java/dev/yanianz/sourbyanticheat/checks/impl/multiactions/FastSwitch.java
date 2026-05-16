package dev.yanianz.sourbyanticheat.checks.impl.multiactions;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;

@CheckData(name = "FastSwitch", stableKey = "sac.multiactions.fastswitch", description = "Detects rapid item switching", setback = 5, decay = 0.02)
public class FastSwitch extends Check implements PacketCheck {

    private long lastSwitch = 0;
    private int switchCount = 0;
    private static final long MIN_SWITCH_MS = 80;

    public FastSwitch(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.HELD_ITEM_CHANGE) return;

        WrapperPlayClientHeldItemChange packet = new WrapperPlayClientHeldItemChange(event);
        long now = System.currentTimeMillis();

        if (lastSwitch > 0) {
            long elapsed = now - lastSwitch;
            if (elapsed < MIN_SWITCH_MS) {
                switchCount++;
                if (switchCount > 5) {
                    flagAndAlert("delay=" + elapsed + "ms count=" + switchCount);
                }
            } else {
                switchCount = Math.max(0, switchCount - 1);
                if (switchCount < 2) reward();
            }
        }
        lastSwitch = now;
    }
}
