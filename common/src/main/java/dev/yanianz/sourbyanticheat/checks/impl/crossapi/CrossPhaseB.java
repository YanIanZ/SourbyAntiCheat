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
import java.util.ArrayList;
import java.util.List;

@CheckData(name = "CrossPhaseB", configName = "crossphaseb", decay = 0.02, setback = 5, stableKey = "cross.phase_b")
public class CrossPhaseB extends Check implements PostPredictionCheck {

    private int buffer;
    private SimpleCollisionBox lastBox;
    private final List<SimpleCollisionBox> collisionBoxes = new ArrayList<>();

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double nettyRateThreshold = 15.0;
    private int blocksPassedThreshold = 2;

    public CrossPhaseB(SacPlayer player) {
        super(player);
        lastBox = player.boundingBox.copy();
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        nettyRateThreshold    = config.getDoubleElse(base + "netty-rate-threshold", 15.0);
        blocksPassedThreshold = config.getIntElse(base + "blocks-passed-threshold", 2);
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

        if (blocksPassed < blocksPassedThreshold) { buffer = Math.max(0, buffer - 1); reward(); return; }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec < nettyRateThreshold;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "Phase");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 3) {
            flagAndAlert(String.format("blocks=%d netty=%.1f/s spartan=%s",
                blocksPassed, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
            return;
        }
        reward();
    }

    /**
     * Swept-AABB test: does the player's bounding box, travelling in a straight line from
     * {@code oldBox} to {@code newBox}, pass through {@code block}?
     *
     * The player box has constant extents, so the swept volume is found by expanding the block
     * by the player box's half-extents (Minkowski sum) and ray-casting the player box centre
     * from its old position to its new position against that expanded block (slab method).
     */
    private static boolean lineIntersects(SimpleCollisionBox newBox, SimpleCollisionBox oldBox, SimpleCollisionBox block) {
        double halfX = (oldBox.maxX - oldBox.minX) / 2.0;
        double halfY = (oldBox.maxY - oldBox.minY) / 2.0;
        double halfZ = (oldBox.maxZ - oldBox.minZ) / 2.0;

        double ox = (oldBox.minX + oldBox.maxX) / 2.0;
        double oy = (oldBox.minY + oldBox.maxY) / 2.0;
        double oz = (oldBox.minZ + oldBox.maxZ) / 2.0;
        double nx = (newBox.minX + newBox.maxX) / 2.0;
        double ny = (newBox.minY + newBox.maxY) / 2.0;
        double nz = (newBox.minZ + newBox.maxZ) / 2.0;

        // Block expanded by the player half-extents.
        double exMinX = block.minX - halfX, exMaxX = block.maxX + halfX;
        double exMinY = block.minY - halfY, exMaxY = block.maxY + halfY;
        double exMinZ = block.minZ - halfZ, exMaxZ = block.maxZ + halfZ;

        double dx = nx - ox, dy = ny - oy, dz = nz - oz;

        double tMin = 0.0, tMax = 1.0;

        // X slab
        if (Math.abs(dx) < 1e-9) {
            if (ox < exMinX || ox > exMaxX) return false;
        } else {
            double t1 = (exMinX - ox) / dx, t2 = (exMaxX - ox) / dx;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        }
        // Y slab
        if (Math.abs(dy) < 1e-9) {
            if (oy < exMinY || oy > exMaxY) return false;
        } else {
            double t1 = (exMinY - oy) / dy, t2 = (exMaxY - oy) / dy;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        }
        // Z slab
        if (Math.abs(dz) < 1e-9) {
            if (oz < exMinZ || oz > exMaxZ) return false;
        } else {
            double t1 = (exMinZ - oz) / dz, t2 = (exMaxZ - oz) / dz;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        }

        return true;
    }
}
