package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

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

    private int spiderBuffer;
    private int consecutiveWallTicks;
    private static final double MIN_Y_DELTA = 0.15;
    private static final double MAX_Y_DELTA = 0.5;
    private static final double NETTY_RATE_THRESHOLD = 20.0;

    public CrossSpider(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

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

        if (againstWall && deltaY > MIN_Y_DELTA && deltaY < MAX_Y_DELTA) {
            consecutiveWallTicks++;
        } else {
            consecutiveWallTicks = 0;
        }

        boolean wallClimbing = consecutiveWallTicks >= 4
            && deltaY > MIN_Y_DELTA && deltaY < MAX_Y_DELTA
            && againstWall;

        if (!wallClimbing) {
            spiderBuffer = Math.max(0, spiderBuffer - 1);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "Spider");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (nettyConfirms || spartanConfirms) {
            spiderBuffer += 2;
        } else {
            spiderBuffer += 1;
        }

        if (spiderBuffer > 4) {
            flagAndAlertWithSetback(String.format("dy=%.3f hCol=%s netty=%.1f/s spartan=%s",
                deltaY, player.horizontalCollision,
                player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
