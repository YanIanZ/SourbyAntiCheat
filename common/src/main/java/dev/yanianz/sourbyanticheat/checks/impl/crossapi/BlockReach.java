package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.BlockPlaceCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockPlace;

@CheckData(name = "BlockReach", configName = "blockreach", decay = 0.02, setback = 10, stableKey = "cross.blockreach")
public class BlockReach extends BlockPlaceCheck {

    private int buffer;
    private static final double MAX_BLOCK_REACH = 5.5;

    public BlockReach(SacPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        double dx = player.x - place.position.getX();
        double dy = player.y + 1.62 - place.position.getY();
        double dz = player.z - place.position.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist < MAX_BLOCK_REACH) { buffer = Math.max(0, buffer - 1); reward(); return; }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > 15.0;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "BlockReach");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 3) {
            flagAndAlert(String.format("dist=%.2f netty=%.1f/s spartan=%s",
                dist, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
