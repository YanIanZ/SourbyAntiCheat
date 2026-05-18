package dev.yanianz.sourbyanticheat.checks.impl.combat;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.MessageUtil;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

import java.util.ArrayList;

@CheckData(name = "MultiInteractB", stableKey = "sac.multiinteract.interact_at_position_changed", decay = 0.02)
public class MultiInteractB extends Check implements PostPredictionCheck {
    private final ArrayList<String> flags = new ArrayList<>();
    private Vector3d lastPos;
    private boolean hasInteracted;
    // Deferred reset: clearing hasInteracted directly in onPacketReceive can wipe a flag
    // set earlier in the same packet invocation. We instead arm the reset and apply it
    // at the start of the next invocation.
    private boolean pendingReset;

    private static final double EPSILON = 0.001;
    // Minimum reliable ticking window before deferred flags may be raised.
    private static final int MIN_RELIABLE_TICKS = 3;

    private static boolean positionEquals(Vector3d a, Vector3d b) {
        return Math.abs(a.x - b.x) < EPSILON
            && Math.abs(a.y - b.y) < EPSILON
            && Math.abs(a.z - b.z) < EPSILON;
    }

    public MultiInteractB(final SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Apply the deferred reset armed by the previous invocation, before processing
        // this packet — so a flag and its reset never collapse into one invocation.
        if (pendingReset) {
            hasInteracted = false;
            pendingReset = false;
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT) return;

            Vector3d pos = packet.getLocation();
            if (pos == null) return; // shouldn't ever happen, but whatever

            if (hasInteracted && !positionEquals(pos, lastPos)) {
                String verbose = "pos=" + MessageUtil.toUnlabledString(pos) + ", lastPos=" + MessageUtil.toUnlabledString(lastPos);
                if (!player.canSkipTicks()) {
                    if (flagAndAlert(verbose) && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    flags.add(verbose);
                }
            }

            lastPos = pos;
            hasInteracted = true;
        }

        // Arm the reset for the next invocation rather than clearing immediately,
        // so a set+reset within this same invocation cannot lose a flag.
        if (!player.cameraEntity.isSelf() || isTickPacket(event.getPacketType())) {
            pendingReset = true;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        // When ticks aren't skippable, flagging happens inline in onPacketReceive.
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(MIN_RELIABLE_TICKS)) {
            if (flags.isEmpty()) {
                reward();
            } else {
                for (String verbose : flags) {
                    flagAndAlert(verbose);
                }
            }
        }

        flags.clear();
    }
}
