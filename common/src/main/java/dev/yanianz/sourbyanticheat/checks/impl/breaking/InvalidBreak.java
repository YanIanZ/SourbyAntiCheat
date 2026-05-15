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
    public InvalidBreak(SacPlayer player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.faceId == 255 && blockBreak.action == DiggingAction.CANCELLED_DIGGING && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10)) {
            return;
        }

        if (blockBreak.faceId < 0 || blockBreak.faceId > 5) {
            // ban
            if (flagAndAlert("face=" + blockBreak.faceId + ", action=" + blockBreak.action) && shouldModifyPackets()) {
                blockBreak.cancel();
            }
        }
    }
}
