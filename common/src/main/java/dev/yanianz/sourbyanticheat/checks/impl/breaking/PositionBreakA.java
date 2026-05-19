package dev.yanianz.sourbyanticheat.checks.impl.breaking;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.BlockBreakCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockBreak;
import dev.yanianz.sourbyanticheat.utils.collisions.datatypes.SimpleCollisionBox;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

@CheckData(name = "PositionBreakA", stableKey = "sac.breaking.position_break_a")
public class PositionBreakA extends Check implements BlockBreakCheck {
    public PositionBreakA(SacPlayer player) {
        super(player);
    }

    // Flat / hitbox-less blocks: their collision box does not match the visual face,
    // so a face-vs-eye-position trace would falsely flag a legitimate break.
    private static boolean isExemptBlock(StateType type) {
        return type == StateTypes.REDSTONE_WIRE
                || type == StateTypes.TRIPWIRE
                || type == StateTypes.RAIL
                || type == StateTypes.POWERED_RAIL
                || type == StateTypes.DETECTOR_RAIL
                || type == StateTypes.ACTIVATOR_RAIL
                || type == StateTypes.VINE;
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (player.inVehicle()
                || blockBreak.action == DiggingAction.CANCELLED_DIGGING
                || isExemptBlock(blockBreak.block.getType())
        ) return;

        SimpleCollisionBox combined = blockBreak.getCombinedBox();

        final double[] possibleEyeHeights = player.getPossibleEyeHeights();
        double minEyeHeight = Double.MAX_VALUE;
        double maxEyeHeight = Double.MIN_VALUE;
        for (double height : possibleEyeHeights) {
            minEyeHeight = Math.min(minEyeHeight, height);
            maxEyeHeight = Math.max(maxEyeHeight, height);
        }

        SimpleCollisionBox eyePositions = new SimpleCollisionBox(player.x, player.y + minEyeHeight, player.z, player.x, player.y + maxEyeHeight, player.z);
        // Always expand by the movement threshold: under lag the tracked position can be stale
        // relative to where the client actually broke from, so the expansion must be unconditional.
        eyePositions.expand(player.getMovementThreshold());

        // If the player is inside a block, then they can ray trace through the block and hit the other side of the block
        if (eyePositions.isIntersected(combined)) {
            reward();
            return;
        }

        // So now we have the player's possible eye positions
        // So then look at the face that the player has clicked
        boolean flag = switch (blockBreak.face) {
            case NORTH -> eyePositions.minZ > combined.minZ; // Z- face
            case SOUTH -> eyePositions.maxZ < combined.maxZ; // Z+ face
            case EAST -> eyePositions.maxX < combined.maxX; // X+ face
            case WEST -> eyePositions.minX > combined.minX; // X- face
            case UP -> eyePositions.maxY < combined.maxY; // Y+ face
            case DOWN -> eyePositions.minY > combined.minY; // Y- face
            default -> false;
        };

        if (!flag) {
            reward();
            return;
        }

        if (flagAndAlert("action=" + blockBreak.action + ", face=" + blockBreak.face) && shouldModifyPackets()) {
            blockBreak.cancel();
        }
    }
}
