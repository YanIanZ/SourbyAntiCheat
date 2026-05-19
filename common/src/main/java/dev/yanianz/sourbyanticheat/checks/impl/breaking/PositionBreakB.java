package dev.yanianz.sourbyanticheat.checks.impl.breaking;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.BlockBreakCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;

@CheckData(name = "PositionBreakB", stableKey = "sac.breaking.position_break_b")
public class PositionBreakB extends Check implements BlockBreakCheck {
    // Face id sent by the client when releasing a dig (CANCELLED_DIGGING):
    // 1.8+ clients send 0, 1.7 and older send 255.
    private static final int RELEASE_FACE_MODERN = 0;
    private static final int RELEASE_FACE_LEGACY = 255;

    private final int releaseFace = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8)
            ? RELEASE_FACE_MODERN : RELEASE_FACE_LEGACY;
    private BlockFace lastFace;
    private int buffer;

    private int flagThreshold = 3;

    public PositionBreakB(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.flagThreshold = config.getIntElse(base + "flag-threshold", 3);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        // A teleport leaves stale digging state — a mismatched face after it is not the player's fault.
        if (player.packetStateData.lastPacketWasTeleport) {
            lastFace = null;
        }

        if (blockBreak.action == DiggingAction.START_DIGGING) {
            if (blockBreak.face == lastFace) {
                lastFace = null;
            }
        }

        if (lastFace != null) {
            // Buffer mismatched faces: a single one can occur from lag/packet ordering,
            // only a sustained run is a genuine violation.
            if (++buffer >= flagThreshold) {
                flagAndAlert("lastFace=" + lastFace + ", action=" + blockBreak.action + ", buffer=" + buffer);
            }
        } else {
            buffer = Math.max(0, buffer - 1);
            reward();
        }

        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) {
            lastFace = blockBreak.faceId == releaseFace ? null : blockBreak.face;
        }
    }
}
