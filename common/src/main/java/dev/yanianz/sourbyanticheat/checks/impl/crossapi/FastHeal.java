package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "FastHeal", configName = "fastheal", decay = 0.02, setback = 5, stableKey = "cross.fastheal")
public class FastHeal extends Check implements PacketCheck {

    private int healCount;
    private long lastReset;
    private int buffer;
    private static final int HEAL_THRESHOLD = 5;

    public FastHeal(SacPlayer player) {
        super(player);
        lastReset = System.currentTimeMillis();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        long now = System.currentTimeMillis();
        if (now - lastReset > 1000) { healCount = 0; lastReset = now; }

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            healCount++;
        }

        if (healCount < HEAL_THRESHOLD) { reward(); return; }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > 15.0;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "FastHeal");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 3) {
            flagAndAlert(String.format("useItems=%d/s netty=%.1f/s spartan=%s",
                healCount, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
