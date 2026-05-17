package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.nmsutil.Collisions;

@CheckData(name = "CrossFastLadder", configName = "crossfastladder", decay = 0.02, setback = 10, stableKey = "cross.fastladder")
public class CrossFastLadder extends Check implements PacketCheck {

    private int buffer;
    private static final double MAX_LADDER_SPEED = 0.20;
    private static final double NETTY_RATE_THRESHOLD = 18.0;

    public CrossFastLadder(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.canFly || player.isFlying || player.isGliding || player.inVehicle()
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE) return;

        boolean onLadder = Collisions.hasMaterial(player,
                player.boundingBox.copy(),
                data -> data.first().getType() == StateTypes.LADDER
                    || data.first().getType() == StateTypes.VINE);

        if (!onLadder) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        double deltaY = player.y - player.lastY;

        if (deltaY > MAX_LADDER_SPEED && deltaY < 0.5) {
            boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

            SpartanCrossCheck.CrossCheckResult spartanResult =
                SpartanCrossCheck.checkSpartan(player.uuid, "FastLadder");
            boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

            buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
            if (buffer > 3) {
                flagAndAlert(String.format("dY=%.3f netty=%.1f/s spartan=%s",
                    deltaY, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
            }
        } else {
            buffer = Math.max(0, buffer - 1);
            reward();
        }
    }
}
