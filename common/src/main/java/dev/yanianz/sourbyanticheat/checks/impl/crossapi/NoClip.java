package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import dev.yanianz.sourbyanticheat.utils.collisions.datatypes.SimpleCollisionBox;
import dev.yanianz.sourbyanticheat.utils.nmsutil.Collisions;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;

import java.util.ArrayList;
import java.util.List;

@CheckData(name = "NoClip", configName = "noclip", decay = 0.05, setback = 10, stableKey = "cross.noclip")
public class NoClip extends Check implements PostPredictionCheck {

    private int insideBuffer;
    private SimpleCollisionBox lastBox;
    private static final double NETTY_RATE_THRESHOLD = 18.0;

    public NoClip(SacPlayer player) {
        super(player);
        lastBox = player.boundingBox.copy();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (player.packetStateData.lastPacketWasTeleport) {
            lastBox = player.boundingBox.copy();
            insideBuffer = 0;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        if (player.compensatedEntities.self.isDead
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE) {
            insideBuffer = 0;
            lastBox = player.boundingBox.copy();
            return;
        }

        SimpleCollisionBox newBox = player.boundingBox.copy();
        boolean phasedThrough = false;

        List<SimpleCollisionBox> boxes = new ArrayList<>();
        Collisions.getCollisionBoxes(player, newBox, boxes, false);

        for (SimpleCollisionBox box : boxes) {
            if (newBox.isIntersected(box) && !lastBox.isIntersected(box)) {
                phasedThrough = true;
                break;
            }
        }

        lastBox = newBox;

        if (!phasedThrough) {
            insideBuffer = Math.max(0, insideBuffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD
            || player.crossValidationData.nettyAvgDelayBetweenPacketsMs < 40.0;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Phase");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            insideBuffer += 2;
        } else {
            insideBuffer += 1;
        }

        if (insideBuffer > 2) {
            flagAndAlert(String.format("netty=%.1f/s spartan=%s",
                player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
