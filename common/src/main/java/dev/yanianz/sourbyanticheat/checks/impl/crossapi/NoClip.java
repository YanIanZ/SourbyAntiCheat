package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
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
    private boolean lastTickPhased;
    private SimpleCollisionBox lastBox;
    private static final double NETTY_RATE_THRESHOLD = 120.0;
    private final List<SimpleCollisionBox> collisionBoxes = new ArrayList<>();

    // Config-wired threshold (raised above the prior hardcoded 2 to absorb
    // a single false collision from a server-placed-block race).
    private int insideBufferThreshold = 4;

    public NoClip(SacPlayer player) {
        super(player);
        lastBox = player.boundingBox.copy();
    }

    @Override
    public void onReload(ConfigManager config) {
        insideBufferThreshold = config.getIntElse(getConfigName() + ".inside-buffer-threshold", 4);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (player.packetStateData.lastPacketWasTeleport) {
            lastBox = player.boundingBox.copy();
            insideBuffer = 0;
            lastTickPhased = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        if (player.compensatedEntities.self.isDead
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE) {
            insideBuffer = 0;
            lastTickPhased = false;
            lastBox = player.boundingBox.copy();
            return;
        }

        SimpleCollisionBox newBox = player.boundingBox.copy();
        boolean phasedThrough = false;

        List<SimpleCollisionBox> boxes = collisionBoxes;
        boxes.clear();
        Collisions.getCollisionBoxes(player, newBox, boxes, false);

        for (SimpleCollisionBox box : boxes) {
            if (newBox.isIntersected(box) && !lastBox.isIntersected(box)) {
                phasedThrough = true;
                break;
            }
        }

        lastBox = newBox;

        // Require the intersection to persist across 2+ consecutive ticks. A single
        // isolated intersection is almost always a server-placed-block race (a block
        // appears adjacent to the player on one tick), not a phase.
        boolean persistentPhase = phasedThrough && lastTickPhased;
        lastTickPhased = phasedThrough;

        if (!persistentPhase) {
            if (!phasedThrough) insideBuffer = Math.max(0, insideBuffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD
            || player.crossValidationData.nettyAvgDelayBetweenPacketsMs < 50.0;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Phase");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            insideBuffer += 2;
        } else {
            insideBuffer += 1;
        }

        if (insideBuffer > insideBufferThreshold) {
            flagAndAlert(String.format("netty=%.1f/s spartan=%s",
                player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
