package dev.yanianz.sourbyanticheat.checks.impl.movement;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.nmsutil.Collisions;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

@CheckData(name = "Step", stableKey = "sac.movement.step", description = "Detects step/vault hacks", setback = 10, decay = 0.02)
public class Step extends Check implements PacketCheck {

    private double stepBuffer = 0;

    // Config-wired thresholds (defaults equal prior hardcoded values).
    // maxStep raised above the prior 0.63: 1.9+ step-assist over a slab/stair pair
    // can legitimately produce a single-tick dY just above 0.63. 0.70 still sits well
    // below any genuine step hack (which vaults a full block, dY ~1.0+).
    private double maxStep = 0.70;
    private double flagThreshold = 5.0;
    private double stepFlagIncrement = 1.0;

    public Step(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.maxStep = config.getDoubleElse(base + "max-step", 0.70);
        this.flagThreshold = config.getDoubleElse(base + "flag-threshold", 5.0);
        this.stepFlagIncrement = config.getDoubleElse(base + "step-flag-increment", 1.0);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.canFly || player.isFlying || player.isGliding || player.inVehicle()
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.JUMP_BOOST)
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.LEVITATION)) return;

        // Slime/honey bounces and an active riptide spin can legitimately produce a large
        // single-tick upward delta — exempt to avoid false positives.
        if (player.riptideSpinAttackTicks > 0) {
            stepBuffer = Math.max(0, stepBuffer - 1);
            reward();
            return;
        }
        boolean bounceExempt = Collisions.hasMaterial(player,
                player.boundingBox.copy().expand(0.1),
                data -> data.first().getType() == StateTypes.SLIME_BLOCK
                    || data.first().getType() == StateTypes.HONEY_BLOCK);
        if (bounceExempt) {
            stepBuffer = Math.max(0, stepBuffer - 1);
            reward();
            return;
        }

        double deltaY = player.y - player.lastY;

        if (deltaY > maxStep && deltaY < 5.0) {
            // Accumulate a sustained-run buffer instead of flagging on the 2nd consecutive
            // step — 1.9+ step-assist legitimately produces a couple of consecutive steps.
            stepBuffer += stepFlagIncrement;
            if (stepBuffer > flagThreshold) {
                flagAndAlert("dY=" + String.format("%.3f", deltaY) + " buffer=" + String.format("%.2f", stepBuffer));
                return;
            }
            reward();
        } else {
            stepBuffer = Math.max(0, stepBuffer - 1);
            reward();
        }
    }
}
