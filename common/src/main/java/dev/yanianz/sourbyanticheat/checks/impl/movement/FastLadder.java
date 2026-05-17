package dev.yanianz.sourbyanticheat.checks.impl.movement;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.nmsutil.Collisions;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;

@CheckData(name = "FastLadder", stableKey = "sac.movement.fastladder", description = "Detects fast ladder climbing", setback = 10, decay = 0.02)
public class FastLadder extends Check implements PacketCheck {

    private static final double MAX_LADDER_SPEED = 0.20;
    private double ladderBuffer = 0;

    public FastLadder(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.canFly || player.isFlying || player.isGliding || player.inVehicle()
                || player.compensatedEntities.self.hasPotionEffect(PotionTypes.LEVITATION)) return;

        boolean onLadder = Collisions.hasMaterial(player,
                player.boundingBox.copy(),
                data -> data.first().getType() == StateTypes.LADDER
                    || data.first().getType() == StateTypes.VINE);

        if (!onLadder) {
            ladderBuffer = Math.max(0, ladderBuffer - 0.02);
            if (ladderBuffer < 0.01) reward();
            return;
        }

        double deltaY = player.y - player.lastY;

        if (deltaY > MAX_LADDER_SPEED && deltaY < 0.5) {
            ladderBuffer += deltaY - MAX_LADDER_SPEED;
            if (ladderBuffer > 0.3) {
                flagAndAlert("dY=" + String.format("%.3f", deltaY) + " buffer=" + String.format("%.3f", ladderBuffer));
            }
        } else {
            ladderBuffer = Math.max(0, ladderBuffer - 0.02);
            if (ladderBuffer < 0.01) reward();
        }
    }
}
