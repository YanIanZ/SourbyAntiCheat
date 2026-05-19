package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsAF", stableKey = "sac.badpackets.ground_spoof_flying", description = "Detects ground spoof mismatch in flying packets", setback = 10)
public class BadPacketsAF extends Check implements PacketCheck {

    private double groundSpoofThreshold = 0.05;
    private double buffer;

    public BadPacketsAF(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        groundSpoofThreshold = config.getDoubleElse(getConfigName() + ".ground-spoof-threshold", 0.05);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport
            || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate) return;
        if (player.isFlying || player.canFly || player.isGliding || player.inVehicle()) return;
        // Slow Falling descends slowly, and a scaffold/ladder descent legitimately
        // claims near-ground state — exempt both to avoid false positives.
        if (player.isClimbing || player.compensatedEntities.self.hasPotionEffect(PotionTypes.SLOW_FALLING)) return;

        WrapperPlayClientPlayerFlying packet = new WrapperPlayClientPlayerFlying(event);
        if (!packet.hasPositionChanged()) return;

        double deltaY = player.y - player.lastY;
        boolean claimedGround = packet.isOnGround();

        if (claimedGround && Math.abs(deltaY) > groundSpoofThreshold && deltaY < 0 && !player.lastOnGround) {
            buffer += 1.0;
            if (buffer > 4.0) {
                flagAndAlert("dY=" + String.format("%.3f", deltaY) + " claimedGround=true");
            }
        } else {
            buffer = Math.max(0, buffer - 1.0);
            reward();
        }
    }
}
