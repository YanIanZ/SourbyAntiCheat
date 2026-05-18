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
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "NoWeb", stableKey = "sac.movement.noweb", description = "Detects ignoring cobweb slowdown", setback = 10, decay = 0.02)
public class NoWeb extends Check implements PacketCheck {

    private double webBuffer = 0;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double maxWebSpeed = 0.08;
    private double secondaryThreshold = 0.15;
    private double bufferDecay = 0.005;
    private double rewardGateThreshold = 0.01;

    public NoWeb(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.maxWebSpeed = config.getDoubleElse(base + "max-web-speed", 0.08);
        this.secondaryThreshold = config.getDoubleElse(base + "secondary-threshold", 0.15);
        this.bufferDecay = config.getDoubleElse(base + "buffer-decay", 0.005);
        this.rewardGateThreshold = config.getDoubleElse(base + "reward-gate-threshold", 0.01);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.canFly || player.isFlying || player.isGliding || player.inVehicle()) return;

        // Speed potion / soul-speed boots / dolphin's grace all legitimately raise speed
        // and would otherwise mask or confuse cobweb-slowdown detection — exempt.
        if (player.compensatedEntities.self.hasPotionEffect(PotionTypes.SPEED)
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.DOLPHINS_GRACE)
                || player.inventory.getBoots().getEnchantmentLevel(EnchantmentTypes.SOUL_SPEED) > 0) {
            webBuffer = Math.max(0, webBuffer - bufferDecay);
            reward();
            return;
        }

        // Only accumulate while the player is genuinely inside a cobweb — otherwise normal
        // walking (deltaH ~0.217) would grow the buffer and false-flag.
        boolean inWeb = Collisions.hasMaterial(player,
                player.boundingBox.copy(),
                data -> data.first().getType() == StateTypes.COBWEB);
        if (!inWeb) {
            webBuffer = Math.max(0, webBuffer - bufferDecay);
            reward();
            return;
        }

        double deltaX = Math.abs(player.x - player.lastX);
        double deltaZ = Math.abs(player.z - player.lastZ);
        double deltaH = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (deltaH > maxWebSpeed && deltaH < 0.5) {
            webBuffer += deltaH - maxWebSpeed;
            if (webBuffer > secondaryThreshold) {
                flagAndAlert("h=" + String.format("%.3f", deltaH) + " buf=" + String.format("%.3f", webBuffer));
            }
        } else {
            webBuffer = Math.max(0, webBuffer - bufferDecay);
            if (webBuffer < rewardGateThreshold) reward();
        }
    }
}
