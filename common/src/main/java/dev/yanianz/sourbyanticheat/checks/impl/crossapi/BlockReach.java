package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.BlockPlaceCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockPlace;
import dev.yanianz.sourbyanticheat.utils.viaversion.ViaVersionUtil;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

@CheckData(name = "BlockReach", configName = "blockreach", decay = 0.02, setback = 10, stableKey = "cross.blockreach")
public class BlockReach extends BlockPlaceCheck {

    private int buffer;
    private double maxBlockReach = 5.5;
    private static final double NETTY_RATE_THRESHOLD = 120.0;

    // Protocol/physics constants — not configurable
    private static final double STANDING_EYE_HEIGHT = 1.62;

    // Cross-version clients report a different eye height / cursor position than the
    // server's pose table — give the reach distance extra headroom before flagging.
    private static final double CROSS_VERSION_LENIENCY = 1.5;

    public BlockReach(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        this.maxBlockReach = config.getDoubleElse(getConfigName() + ".max-block-reach", 5.5);
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.compensatedEntities.self.isDead) return;

        // Teleport exemption — position is stale immediately after teleport
        if (player.packetStateData.lastPacketWasTeleport) { reward(); return; }

        // Use pose-aware eye height (crouching/swimming have lower eye heights)
        double eyeHeight = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)
                ? player.pose.eyeHeight
                : STANDING_EYE_HEIGHT;

        double dx = player.x - place.position.getX();
        double dy = player.y + eyeHeight - place.position.getY();
        double dz = player.z - place.position.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        double effectiveMaxBlockReach = maxBlockReach * (ViaVersionUtil.isCrossVersion(player) ? CROSS_VERSION_LENIENCY : 1.0);

        if (dist < effectiveMaxBlockReach) { buffer = Math.max(0, buffer - 1); reward(); return; }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "BlockReach");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 3) {
            flagAndAlert(String.format("dist=%.2f netty=%.1f/s spartan=%s",
                dist, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
            return;
        }
    }
}
