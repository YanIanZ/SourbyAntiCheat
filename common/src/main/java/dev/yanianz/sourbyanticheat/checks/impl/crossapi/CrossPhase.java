package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.collisions.datatypes.SimpleCollisionBox;
import dev.yanianz.sourbyanticheat.utils.nmsutil.Collisions;

@CheckData(name = "CrossPhase", configName = "crossphase", decay = 0.05, setback = 10, stableKey = "cross.phase")
public class CrossPhase extends Check implements PacketCheck {

    private int phaseBuffer;
    private long lastPacketTime;
    private static final long GAP_THRESHOLD_MS = 500;

    public CrossPhase(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        if (player.packetStateData.lastPacketWasTeleport) {
            phaseBuffer = 0;
            return;
        }

        long now = System.currentTimeMillis();
        long gap = now - lastPacketTime;
        lastPacketTime = now;

        boolean gapFlag = gap > GAP_THRESHOLD_MS;

        SimpleCollisionBox playerBox = player.boundingBox.copy().expand(0.1);
        boolean insideBlock = !Collisions.isEmpty(player, playerBox);

        if (!gapFlag && !insideBlock) {
            phaseBuffer = Math.max(0, phaseBuffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec < 15.0
            || gap > GAP_THRESHOLD_MS * 2;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Phase");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        String verbose;
        if (insideBlock) {
            verbose = String.format("inside-block netty=%.1f/s spartan=%s",
                player.crossValidationData.nettyPacketRatePerSec, spartanResult.type());
        } else {
            verbose = String.format("gap=%dms netty=%.1f/s spartan=%s",
                gap, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type());
        }

        if (nettyConfirms || spartanConfirms) {
            phaseBuffer += 2;
        } else {
            phaseBuffer += 1;
        }

        if (phaseBuffer > 3) {
            flagAndAlertWithSetback(verbose);
        }
    }
}