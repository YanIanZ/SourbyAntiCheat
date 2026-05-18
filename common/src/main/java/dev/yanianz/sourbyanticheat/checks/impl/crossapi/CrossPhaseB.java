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
    private final List<SimpleCollisionBox> collisionBoxes = new ArrayList<>();

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double nettyRateThreshold = 15.0;
    private int blocksPassedThreshold = 2;

    public CrossPhaseB(SacPlayer player) {
        super(player);
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
            return;
        }

        SimpleCollisionBox playerBox = player.boundingBox.copy();
        List<SimpleCollisionBox> boxes = collisionBoxes;
        boxes.clear();
        Collisions.getCollisionBoxes(player, playerBox, boxes, false);

        int blocksPassed = 0;
        for (SimpleCollisionBox box : boxes) {
            if (isEmbeddedIn(playerBox, box)) { blocksPassed++; }
        }

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
     * Phase test: is the player's bounding box genuinely embedded inside a solid block?
     *
     * Returns true only when {@code playerBox} overlaps {@code block} with positive volume on
     * every axis (beyond a small epsilon). A player resting on a floor or pressed against a
     * wall only *touches* the block — zero overlap on the contact axis — and is NOT counted.
     * Only a box that has penetrated a solid block's interior, i.e. actual phasing, matches.
     *
     * This replaces a Minkowski swept-AABB test that reported true for mere contact, flagging
     * every player walking near walls or standing on the ground.
     */
    private static boolean isEmbeddedIn(SimpleCollisionBox playerBox, SimpleCollisionBox block) {
        final double eps = 1.0e-3;
        return playerBox.minX < block.maxX - eps && playerBox.maxX > block.minX + eps
            && playerBox.minY < block.maxY - eps && playerBox.maxY > block.minY + eps
            && playerBox.minZ < block.maxZ - eps && playerBox.maxZ > block.minZ + eps;
    }
}
