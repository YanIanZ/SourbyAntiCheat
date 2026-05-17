package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.collisions.datatypes.SimpleCollisionBox;
import dev.yanianz.sourbyanticheat.utils.nmsutil.Collisions;

@CheckData(name = "CrossPhase", configName = "crossphase", decay = 0.05, setback = 5, stableKey = "cross.phase")
public class CrossPhase extends Check implements PacketCheck {

    private int phaseBuffer;
    private long lastPacketTime;
    private static final long GAP_THRESHOLD_MS = 1500;
    private static final double OFFSET_THRESHOLD = 0.5;
    private static final double NETTY_RATE_THRESHOLD = 15.0;

    public CrossPhase(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        if (player.packetStateData.lastPacketWasTeleport
                || player.compensatedEntities.self.isDead
                || player.gamemode == GameMode.SPECTATOR
                || player.gamemode == GameMode.CREATIVE) {
            phaseBuffer = 0;
            return;
        }

        long now = System.currentTimeMillis();
        long gap = now - lastPacketTime;
        lastPacketTime = now;

        boolean gapFlag = gap > GAP_THRESHOLD_MS;

        SimpleCollisionBox playerBox = player.boundingBox.copy().expand(0.05);
        boolean insideBlock = !Collisions.isEmpty(player, playerBox)
            && player.crossValidationData.offsetFromPrediction > OFFSET_THRESHOLD;

        if (!gapFlag && !insideBlock) {
            phaseBuffer = Math.max(0, phaseBuffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec < NETTY_RATE_THRESHOLD
            || gap > GAP_THRESHOLD_MS * 2;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Phase");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        String verbose;
        if (insideBlock) {
            verbose = String.format("inside-block off=%.3f netty=%.1f/s spartan=%s",
                player.crossValidationData.offsetFromPrediction,
                player.crossValidationData.nettyPacketRatePerSec, spartanResult.type());
        } else {
            verbose = String.format("gap=%dms netty=%.1f/s spartan=%s",
                gap, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type());
        }

        int ping = player.getTransactionPing();
        double multiplier = ping > 400 ? 0.5 : 1.0;

        if (nettyConfirms || spartanConfirms) {
            phaseBuffer += (int)(2 * multiplier);
        } else {
            phaseBuffer += (int)(1 * multiplier);
        }

        if (phaseBuffer > 8) {
            flagAndAlert(verbose);
        }
    }
}
