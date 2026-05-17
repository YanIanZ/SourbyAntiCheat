package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossFastEat", configName = "crossfasteat", decay = 0.02, setback = 5, stableKey = "cross.fasteat")
public class CrossFastEat extends Check implements PostPredictionCheck {

    private double buffer;
    private long useStartTime = 0;
    private boolean isUsing = false;
    private static final long MIN_EAT_TIME = 1400;
    private static final double NETTY_RATE_THRESHOLD = 15.0;

    public CrossFastEat(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            useStartTime = System.currentTimeMillis();
            isUsing = true;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            var dig = new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging(event);
            if (dig.getAction() == com.github.retrooper.packetevents.protocol.player.DiggingAction.RELEASE_USE_ITEM) {
                isUsing = false;
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        if (!isUsing || useStartTime == 0) {
            buffer = Math.max(0, buffer - 0.02);
            reward();
            return;
        }

        long elapsed = System.currentTimeMillis() - useStartTime;
        if (elapsed > 5000 || elapsed < 100) {
            isUsing = false;
            return;
        }

        boolean fastEat = elapsed < MIN_EAT_TIME;

        if (!fastEat) {
            buffer = Math.max(0, buffer - 0.02);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "FastEat");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 1.5 : 0.5;
        if (buffer > 3.0) {
            flagAndAlert(String.format("eat=%dms netty=%.1f/s spartan=%s",
                elapsed, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
        isUsing = false;
    }
}
