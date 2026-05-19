package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "PortalInventory", configName = "portalinventory", decay = 0.02, setback = 5, stableKey = "cross.portalinventory")
public class PortalInventory extends Check implements PostPredictionCheck {

    private int buffer;
    private boolean inPortal = false;
    private boolean clickedInventory = false;

    // Config-wired threshold (default equals prior hardcoded value)
    private double nettyRateThreshold = 120.0;

    // Fixed physics constant: player eye height (blocks).
    private static final double EYE_HEIGHT = 1.62;

    public PortalInventory(SacPlayer player) { super(player); }

    @Override
    public void onReload(ConfigManager config) {
        nettyRateThreshold = config.getDoubleElse(getConfigName() + ".netty-rate-threshold", 120.0);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW && inPortal) {
            clickedInventory = true;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.compensatedEntities.self.isDead) {
            inPortal = false;
            clickedInventory = false;
            return;
        }

        // Detect the actual nether portal block the player occupies, rather than
        // inferring "portal" from a hovering position delta (which matched any hover).
        inPortal = isInsidePortalBlock();

        if (!clickedInventory) { reward(); return; }
        clickedInventory = false;

        if (!inPortal) { buffer = Math.max(0, buffer - 1); reward(); return; }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > nettyRateThreshold;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "Exploits");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 3) {
            flagAndAlert(String.format("netty=%.1f/s spartan=%s",
                player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }

    private boolean isInsidePortalBlock() {
        int bx = (int) Math.floor(player.x);
        int bz = (int) Math.floor(player.z);
        int feetY = (int) Math.floor(player.y);
        int headY = (int) Math.floor(player.y + EYE_HEIGHT);
        for (int by = feetY; by <= headY; by++) {
            WrappedBlockState block = player.compensatedWorld.getBlock(bx, by, bz);
            if (block != null && block.getType() == StateTypes.NETHER_PORTAL) {
                return true;
            }
        }
        return false;
    }
}
