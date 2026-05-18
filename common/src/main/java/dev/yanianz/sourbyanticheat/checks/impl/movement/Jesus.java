package dev.yanianz.sourbyanticheat.checks.impl.movement;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.nmsutil.Collisions;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "Jesus", stableKey = "sac.movement.jesus", description = "Detects water walking / jesus hacks", setback = 10, decay = 0.02)
public class Jesus extends Check implements PacketCheck {

    private int surfaceTicks = 0;

    // Config-wired thresholds.
    // frac = fractional part of feet Y within a block; a water-walker rests right at the
    // surface (frac ~0.92-0.99). The lower 0.85-0.92 region is where legit high-latency
    // water bobbing drifts, so fracMin defaults to 0.92 to exclude those false positives
    // while still catching a genuine surface-locked walker.
    private double fracMin = 0.92;
    private double fracMax = 0.99;
    private double offsetThreshold = 0.005;
    private int bufferThreshold = 8;
    private int bufferFlag = 5;

    public Jesus(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.fracMin = config.getDoubleElse(base + "frac-min", 0.92);
        this.fracMax = config.getDoubleElse(base + "frac-max", 0.99);
        this.offsetThreshold = config.getDoubleElse(base + "offset-threshold", 0.005);
        this.bufferThreshold = config.getIntElse(base + "buffer-threshold", 8);
        this.bufferFlag = config.getIntElse(base + "buffer-flag", 5);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.compensatedEntities.self.hasPotionEffect(PotionTypes.DOLPHINS_GRACE)) return;
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.canFly || player.isFlying || player.isGliding || player.inVehicle()) return;

        // Boats, lily pads and slime-block bounces let a player legitimately rest at/near
        // the water surface without "walking" on it — exempt to avoid false positives.
        boolean bounceExempt = Collisions.hasMaterial(player,
                player.boundingBox.copy().expand(0.1),
                data -> data.first().getType() == StateTypes.LILY_PAD
                    || data.first().getType() == StateTypes.SLIME_BLOCK);
        if (bounceExempt) {
            surfaceTicks = Math.max(0, surfaceTicks - 2);
            reward();
            return;
        }

        WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
        double deltaY = player.y - player.lastY;
        double frac = player.y - Math.floor(player.y);

        boolean nearSurface = frac > fracMin && frac < fracMax;
        boolean notFalling = deltaY > -offsetThreshold && !flying.isOnGround();

        if (nearSurface && notFalling) {
            surfaceTicks++;
            if (surfaceTicks > bufferThreshold) {
                flagAndAlert("surface=" + String.format("%.3f", frac) + " dY=" + String.format("%.3f", deltaY));
            }
        } else {
            surfaceTicks = Math.max(0, surfaceTicks - 2);
            if (surfaceTicks < bufferFlag) reward();
        }
    }
}
