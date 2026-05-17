package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossNoSlowdown", configName = "crossnoslowdown", decay = 0.02, setback = 10, stableKey = "cross.noslowdown")
public class CrossNoSlowdown extends Check implements PostPredictionCheck {

    private double buffer;
    private boolean isUsingItem = false;
    private long lastUseStart = 0;
    private static final double SPRINT_SPEED = 0.28;
    private static final double NETTY_RATE_THRESHOLD = 20.0;
    private static final long USE_TIMEOUT_MS = 5000;

    public CrossNoSlowdown(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            isUsingItem = true;
            lastUseStart = System.currentTimeMillis();
        }
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            var dig = new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging(event);
            if (dig.getAction() == com.github.retrooper.packetevents.protocol.player.DiggingAction.RELEASE_USE_ITEM) {
                isUsingItem = false;
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        if (player.inVehicle() || player.canFly || player.isGliding
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE) return;

        if (!isUsingItem || System.currentTimeMillis() - lastUseStart > USE_TIMEOUT_MS) {
            isUsingItem = false;
            buffer = Math.max(0, buffer - 0.02);
            reward();
            return;
        }

        double deltaX = player.x - player.lastX;
        double deltaZ = player.z - player.lastZ;
        double speed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        boolean fastWhileUsing = speed > SPRINT_SPEED;

        if (!fastWhileUsing) {
            buffer = Math.max(0, buffer - 0.02);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "NoSlowdown");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            buffer += 1.5;
        } else {
            buffer += 0.5;
        }

        if (buffer > 3.0) {
            flagAndAlert(String.format("speed=%.2f netty=%.1f/s spartan=%s",
                speed, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
