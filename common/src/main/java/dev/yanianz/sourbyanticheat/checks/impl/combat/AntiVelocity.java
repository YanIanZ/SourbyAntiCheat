package dev.yanianz.sourbyanticheat.checks.impl.combat;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

@CheckData(name = "AntiVelocity", stableKey = "sac.combat.antivelocity", description = "Detects anti-knockback / velocity hacks", setback = 15, decay = 0.01)
public class AntiVelocity extends Check implements PacketCheck {

    private int veloFlags = 0;

    public AntiVelocity(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_FLYING
            && event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION
            && event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) return;

        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.inVehicle() || player.isFlying || player.canFly) return;

        double deltaX = Math.abs(player.x - player.lastX);
        double deltaZ = Math.abs(player.z - player.lastZ);

        if (player.likelyKB != null && player.likelyKB.getX() != 0 && player.likelyKB.getZ() != 0) {
            double expectedX = Math.abs(player.likelyKB.getX());
            double expectedZ = Math.abs(player.likelyKB.getZ());
            double reductionX = expectedX > 0.01 ? deltaX / expectedX : 1;
            double reductionZ = expectedZ > 0.01 ? deltaZ / expectedZ : 1;

            if (reductionX < 0.3 && reductionZ < 0.3) {
                veloFlags++;
                if (veloFlags > 3) {
                    flagAndAlert("reduction=" + String.format("%.0f%%", (1 - Math.min(reductionX, reductionZ)) * 100));
                }
            } else {
                veloFlags = Math.max(0, veloFlags - 1);
                if (veloFlags < 1) reward();
            }
        }
    }
}
