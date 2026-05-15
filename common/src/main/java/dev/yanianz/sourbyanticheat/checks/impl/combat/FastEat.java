package dev.yanianz.sourbyanticheat.checks.impl.combat;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

@CheckData(name = "FastEat", stableKey = "sac.combat.fasteat", description = "Detects fast eating/healing", setback = 5, decay = 0.02)
public class FastEat extends Check implements PacketCheck {

    private long lastUseTime = 0;
    private static final long MIN_EAT_DELAY = 1200;
    private int flags = 0;

    public FastEat(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.USE_ITEM) return;

        long now = System.currentTimeMillis();
        if (lastUseTime > 0) {
            long elapsed = now - lastUseTime;
            if (elapsed < MIN_EAT_DELAY) {
                flags++;
                if (flags > 3) {
                    flagAndAlert("delay=" + elapsed + "ms flags=" + flags);
                }
            } else {
                flags = Math.max(0, flags - 1);
                if (flags < 1) reward();
            }
        }
        lastUseTime = now;
    }
}
