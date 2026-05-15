package dev.yanianz.sourbyanticheat.checks.impl.combat;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

@CheckData(name = "FastBow", stableKey = "sac.combat.fastbow", description = "Detects rapid bow shooting", setback = 5, decay = 0.02)
public class FastBow extends Check implements PacketCheck {

    private long lastBowTime = 0;
    private static final long MIN_BOW_DELAY = 800;
    private int flags = 0;

    public FastBow(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT
            && event.getPacketType() != PacketType.Play.Client.USE_ITEM) return;

        long now = System.currentTimeMillis();
        if (lastBowTime > 0) {
            long elapsed = now - lastBowTime;
            if (elapsed < MIN_BOW_DELAY) {
                flags++;
                if (flags > 5) {
                    flagAndAlert("delay=" + elapsed + "ms flags=" + flags);
                }
            } else {
                flags = Math.max(0, flags - 1);
                if (flags < 2) reward();
            }
        }
        lastBowTime = now;
    }
}
