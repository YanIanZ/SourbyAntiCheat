package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "CrossElytraMove", configName = "crosselytramove", decay = 0.05, setback = 12, stableKey = "cross.elytramove")
public class CrossElytraMove extends Check implements PacketCheck {

    private int elytraBuffer;
    private static final double SPEED_THRESHOLD = 30.0;
    private static final double NETTY_RATE_THRESHOLD = 20.0;

    public CrossElytraMove(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        boolean gliding = player.crossValidationData.peGliding;
        if (!gliding) {
            elytraBuffer = Math.max(0, elytraBuffer - 1);
            reward();
            return;
        }

        double dx = player.crossValidationData.pePositionDeltaX;
        double dy = player.crossValidationData.pePositionDeltaY;
        double dz = player.crossValidationData.pePositionDeltaZ;
        double speed = Math.sqrt(dx * dx + dy * dy + dz * dz);

        boolean speedFlag = speed > SPEED_THRESHOLD;

        if (!speedFlag) {
            elytraBuffer = Math.max(0, elytraBuffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "ElytraMove");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            elytraBuffer += 2;
        } else {
            elytraBuffer += 1;
        }

        if (elytraBuffer > 3) {
            flagAndAlertWithSetback(String.format("speed=%.1f netty=%.1f/s spartan=%s",
                speed, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
