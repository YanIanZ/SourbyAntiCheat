package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "AutoLoot", configName = "autoloot", decay = 0.02, setback = 5, stableKey = "cross.autoloot")
public class AutoLoot extends Check implements PacketCheck {

    private int pickupCount;
    private long lastReset;
    private int buffer;
    private static final int PICKUP_THRESHOLD = 10;
    private static final double NETTY_RATE_THRESHOLD = 15.0;

    public AutoLoot(SacPlayer player) {
        super(player);
        lastReset = System.currentTimeMillis();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        long now = System.currentTimeMillis();
        if (now - lastReset > 1000) { pickupCount = 0; lastReset = now; }

        if (event.getPacketType() == PacketType.Play.Client.PICK_ITEM) {
            pickupCount++;
        }

        if (pickupCount < PICKUP_THRESHOLD) return;

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "Exploits");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 3) {
            flagAndAlert(String.format("picks=%d/s netty=%.1f/s spartan=%s",
                pickupCount, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
