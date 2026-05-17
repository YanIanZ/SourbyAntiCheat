package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.BlockPlaceCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockPlace;

@CheckData(name = "CrossFastPlace", configName = "crossfastplace", decay = 0.02, setback = 10, stableKey = "cross.fastplace")
public class CrossFastPlace extends BlockPlaceCheck {

    private int placeCount;
    private long lastReset;
    private int buffer;

    private int placeThreshold = 6;
    private double nettyRateThreshold = 18.0;

    public CrossFastPlace(SacPlayer player) {
        super(player);
        lastReset = System.currentTimeMillis();
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.placeThreshold     = config.getIntElse(base + "place-threshold",       6);
        this.nettyRateThreshold = config.getDoubleElse(base + "netty-rate-threshold", 18.0);
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.compensatedEntities.self.isDead) return;

        long now = System.currentTimeMillis();
        if (now - lastReset > 1000) { placeCount = 0; lastReset = now; }
        placeCount++;

        if (placeCount < placeThreshold) {
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > nettyRateThreshold;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "FastPlace");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 3) {
            flagAndAlert(String.format("places=%d/s netty=%.1f/s spartan=%s",
                placeCount, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
            return;
        }
        reward();
    }
}
