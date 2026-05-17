package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import dev.yanianz.sourbyanticheat.utils.collisions.datatypes.SimpleCollisionBox;
import dev.yanianz.sourbyanticheat.utils.nmsutil.Collisions;
import java.util.ArrayList;
import java.util.List;

@CheckData(name = "CrossPhaseB", configName = "crossphaseb", decay = 0.02, setback = 5, stableKey = "cross.phase_b")
public class CrossPhaseB extends Check implements PostPredictionCheck {

    private int buffer;
    private SimpleCollisionBox lastBox;
    private static final double NETTY_RATE_THRESHOLD = 15.0;
    private final List<SimpleCollisionBox> collisionBoxes = new ArrayList<>();

    public CrossPhaseB(SacPlayer player) {
        super(player);
        lastBox = player.boundingBox.copy();
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;
        if (player.packetStateData.lastPacketWasTeleport || player.compensatedEntities.self.isDead
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) {
            lastBox = player.boundingBox.copy();
            return;
        }

        SimpleCollisionBox newBox = player.boundingBox.copy();
        List<SimpleCollisionBox> boxes = collisionBoxes;
        boxes.clear();
        Collisions.getCollisionBoxes(player, lastBox, boxes, false);

        int blocksPassed = 0;
        for (SimpleCollisionBox box : boxes) {
            if (lineIntersects(newBox, lastBox, box)) { blocksPassed++; }
        }

        lastBox = newBox;

        if (blocksPassed < 2) { buffer = Math.max(0, buffer - 1); reward(); return; }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec < NETTY_RATE_THRESHOLD;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "Phase");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 3) {
            flagAndAlert(String.format("blocks=%d netty=%.1f/s spartan=%s",
                blocksPassed, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }

    private static boolean lineIntersects(SimpleCollisionBox newBox, SimpleCollisionBox oldBox, SimpleCollisionBox block) {
        double minX = Math.min(oldBox.minX, newBox.minX);
        double maxX = Math.max(oldBox.maxX, newBox.maxX);
        double minY = Math.min(oldBox.minY, newBox.minY);
        double maxY = Math.max(oldBox.maxY, newBox.maxY);
        double minZ = Math.min(oldBox.minZ, newBox.minZ);
        double maxZ = Math.max(oldBox.maxZ, newBox.maxZ);
        return block.minX < maxX && block.maxX > minX
            && block.minY < maxY && block.maxY > minY
            && block.minZ < maxZ && block.maxZ > minZ;
    }
}
