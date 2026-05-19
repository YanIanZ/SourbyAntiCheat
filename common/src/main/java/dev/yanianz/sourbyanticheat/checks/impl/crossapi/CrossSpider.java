package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;

@CheckData(name = "CrossSpider", configName = "crossspider", decay = 0.1, setback = 10, stableKey = "cross.spider")
public class CrossSpider extends Check implements PacketCheck {

    private double spiderBuffer;
    private int consecutiveWallTicks;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double minYDelta          = 0.15;
    private double maxYDelta          = 0.5;
    private double nettyRateThreshold = 120.0;

    public CrossSpider(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        minYDelta          = config.getDoubleElse(base + "min-y-delta", 0.15);
        maxYDelta          = config.getDoubleElse(base + "max-y-delta", 0.5);
        nettyRateThreshold = config.getDoubleElse(base + "netty-rate-threshold", 120.0);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        if (player.wasTouchingWater || player.compensatedEntities.self.isDead
                || player.isGliding || player.canFly
                || player.inVehicle()
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.JUMP_BOOST)
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.LEVITATION)) {
            spiderBuffer = Math.max(0, spiderBuffer - 1);
            consecutiveWallTicks = 0;
            reward();
            return;
        }

        double deltaY = player.crossValidationData.pePositionDeltaY;
        boolean againstWall = player.horizontalCollision;

        if (againstWall && deltaY > minYDelta && deltaY < maxYDelta) {
            consecutiveWallTicks++;
        } else {
            consecutiveWallTicks = 0;
        }

        boolean wallClimbing = consecutiveWallTicks >= 4
            && deltaY > minYDelta && deltaY < maxYDelta
            && againstWall;

        if (!wallClimbing) {
            spiderBuffer = Math.max(0, spiderBuffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > nettyRateThreshold;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Spider");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        int ping = player.getTransactionPing();
        double multiplier = ping > 400 ? 0.5 : 1.0;

        if (nettyConfirms || spartanConfirms) {
            spiderBuffer += 2 * multiplier;
        } else {
            spiderBuffer += multiplier;
        }

        if (spiderBuffer > 4) {
            flagAndAlert(String.format("dy=%.3f hCol=%s netty=%.1f/s spartan=%s",
                deltaY, player.horizontalCollision,
                player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
