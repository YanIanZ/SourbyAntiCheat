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

@CheckData(name = "FastLadder", stableKey = "sac.movement.fastladder", description = "Detects fast ladder climbing", setback = 10, decay = 0.02)
public class FastLadder extends Check implements PacketCheck {

    private double ladderBuffer = 0;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double maxLadderSpeed = 0.20;
    private double bufferIncrement = 0.3;
    private double bufferDecay = 0.01;

    public FastLadder(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.maxLadderSpeed = config.getDoubleElse(base + "max-ladder-speed", 0.20);
        this.bufferIncrement = config.getDoubleElse(base + "buffer-increment", 0.3);
        this.bufferDecay = config.getDoubleElse(base + "buffer-decay", 0.01);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.canFly || player.isFlying || player.isGliding || player.inVehicle()) return;

        // Levitation causes uncontrolled vertical movement — exempt.
        if (player.compensatedEntities.self.hasPotionEffect(PotionTypes.LEVITATION)) {
            ladderBuffer = Math.max(0, ladderBuffer - bufferDecay);
            reward();
            return;
        }

        // Confirm the player is actually on/against a climbable block before accumulating —
        // otherwise normal jump arcs (upward Y delta 0.20-0.50) would false-flag.
        boolean onLadder = Collisions.hasMaterial(player,
                player.boundingBox.copy(),
                data -> data.first().getType() == StateTypes.LADDER
                    || data.first().getType() == StateTypes.VINE
                    || data.first().getType() == StateTypes.SCAFFOLDING
                    || data.first().getType() == StateTypes.WEEPING_VINES
                    || data.first().getType() == StateTypes.TWISTING_VINES);

        double deltaY = player.y - player.lastY;

        if (onLadder && deltaY > maxLadderSpeed && deltaY < 0.5) {
            ladderBuffer += deltaY - maxLadderSpeed;
            if (ladderBuffer > bufferIncrement) {
                flagAndAlert("dY=" + String.format("%.3f", deltaY) + " buffer=" + String.format("%.3f", ladderBuffer));
            }
        } else {
            // Clean tick: decay the gate buffer and reward (VL decay). The buffer and the
            // violation counter are distinct variables, so this is one logical decay signal.
            ladderBuffer = Math.max(0, ladderBuffer - bufferDecay);
            reward();
        }
    }
}
