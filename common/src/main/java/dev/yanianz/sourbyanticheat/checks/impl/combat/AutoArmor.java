package dev.yanianz.sourbyanticheat.checks.impl.combat;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

@CheckData(name = "AutoArmor", stableKey = "sac.combat.autoarmor", description = "Detects automatic armor equipping", setback = 5, decay = 0.02, experimental = true)
public class AutoArmor extends Check implements PacketCheck {

    private long lastSlotTime = 0;
    private int fastSwitchCount = 0;
    private static final long MIN_SWITCH_DELAY = 50;

    public AutoArmor(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW
            && event.getPacketType() != PacketType.Play.Client.CLOSE_WINDOW) return;

        long now = System.currentTimeMillis();
        if (lastSlotTime > 0) {
            long elapsed = now - lastSlotTime;
            if (elapsed < MIN_SWITCH_DELAY) {
                fastSwitchCount++;
                if (fastSwitchCount > 10) {
                    flagAndAlert("switch=" + fastSwitchCount + " delay=" + elapsed + "ms");
                }
            } else {
                fastSwitchCount = Math.max(0, fastSwitchCount - 2);
                if (fastSwitchCount < 3) reward();
            }
        }
        lastSlotTime = now;
    }
}
