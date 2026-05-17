package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.BlockBreakCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockBreak;
import dev.yanianz.sourbyanticheat.utils.nmsutil.Materials;

@CheckData(name = "BedFucker", configName = "bedfucker", decay = 0.02, setback = 10, stableKey = "cross.bedfucker")
public class BedFucker extends Check implements BlockBreakCheck {

    private int bedBreakCount;
    private long lastReset;
    private int buffer;
    private static final int BED_THRESHOLD = 3;

    public BedFucker(SacPlayer player) {
        super(player);
        lastReset = System.currentTimeMillis();
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.compensatedEntities.self.isDead) return;

        long now = System.currentTimeMillis();
        if (now - lastReset > 1000) { bedBreakCount = 0; lastReset = now; }

        if (!Materials.isBed(blockBreak.block.getType())) return;

        bedBreakCount++;
        if (bedBreakCount < BED_THRESHOLD) { reward(); return; }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > 15.0;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "FastBreak");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 3) {
            flagAndAlert(String.format("beds=%d/s netty=%.1f/s spartan=%s",
                bedBreakCount, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
