package dev.yanianz.sourbyanticheat.checks.impl.breaking;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.BlockBreakCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;

@CheckData(name = "InvalidBreak", stableKey = "sac.breaking.invalid_break", description = "Sent impossible block face id")
public class InvalidBreak extends Check implements BlockBreakCheck {
    // Highest valid block face id (DOWN..EAST -> 0..5)
    private static final int MAX_FACE_ID = 5;

    public InvalidBreak(SacPlayer player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        // 1.7 clients legitimately send face id 255 for both START_DIGGING and
        // CANCELLED_DIGGING — exempt both so neither is falsely flagged.
        if (blockBreak.faceId == 255
                && (blockBreak.action == DiggingAction.CANCELLED_DIGGING || blockBreak.action == DiggingAction.START_DIGGING)
                && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10)) {
            return;
        }

        if (blockBreak.faceId < 0 || blockBreak.faceId > MAX_FACE_ID) {
            // ban
            if (flagAndAlert("face=" + blockBreak.faceId + ", action=" + blockBreak.action) && shouldModifyPackets()) {
                blockBreak.cancel();
            }
        } else {
            reward();
        }
    }
}
