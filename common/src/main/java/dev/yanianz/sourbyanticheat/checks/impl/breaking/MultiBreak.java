package dev.yanianz.sourbyanticheat.checks.impl.breaking;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.BlockBreakCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.MessageUtil;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockBreak;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3i;

import java.util.ArrayList;
import java.util.List;

@CheckData(name = "MultiBreak", stableKey = "sac.breaking.multi_break")
public class MultiBreak extends Check implements BlockBreakCheck {
    // Upper bound on deferred flags. If the player never ticks reliably,
    // onPredictionComplete keeps early-returning, so without a cap this list
    // would grow unbounded — discard the oldest entries beyond this limit.
    private static final int MAX_DEFERRED_FLAGS = 32;
    private final List<String> flags = new ArrayList<>();
    private boolean hasBroken;
    private BlockFace lastFace;
    private Vector3i lastPos;

    public MultiBreak(SacPlayer player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) {
            return;
        }

        if (hasBroken && (blockBreak.face != lastFace || !blockBreak.position.equals(lastPos))) {
            final String verbose = "face=" + blockBreak.face + ", lastFace=" + lastFace
                    + ", pos=" + MessageUtil.toUnlabledString(blockBreak.position)
                    + ", lastPos=" + MessageUtil.toUnlabledString(lastPos);
            if (!player.canSkipTicks()) {
                if (flagAndAlert(verbose) && shouldModifyPackets()) {
                    blockBreak.cancel();
                }
            } else {
                // Defer the flag; it cannot be cancelled inline because the player is
                // not ticking reliably — it is enforced via setback in onPredictionComplete.
                if (flags.size() >= MAX_DEFERRED_FLAGS) {
                    flags.remove(0);
                }
                flags.add(verbose);
            }
        } else {
            reward();
        }

        lastFace = blockBreak.face;
        lastPos = blockBreak.position;
        hasBroken = true;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Only reset accumulation on a genuine tick boundary. A camera-entity transition
        // (cameraEntity not self) must not by itself wipe hasBroken, otherwise a
        // spectator-camera swap mid-tick would suppress multi-break accumulation.
        if (isTickPacket(event.getPacketType())) {
            hasBroken = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        // Only process and clear deferred flags once the player is ticking reliably.
        // If not, keep the flags queued so they are not silently dropped.
        if (!player.isTickingReliablyFor(3)) return;

        for (String verbose : flags) {
            // Deferred flags get a cancel path via setback (the break packet is long gone).
            flagAndAlertWithSetback(verbose);
        }

        flags.clear();
    }
}
