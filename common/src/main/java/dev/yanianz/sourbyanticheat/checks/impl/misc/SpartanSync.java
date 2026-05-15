package dev.yanianz.sourbyanticheat.checks.impl.misc;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "SpartanSync", stableKey = "sac.spartan.sync", description = "Detects significant SAC vs Spartan VL divergence", setback = 20)
public class SpartanSync extends Check implements PacketCheck {

    private int tickCounter = 0;
    private static final int CHECK_INTERVAL = 100;

    public SpartanSync(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (!SpartanCrossCheck.isAvailable()) return;

        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;

        int totalSAC = getTotalSACVL();
        int totalSpartan = SpartanCrossCheck.getSpartanPerCheckVL(player.uuid, "total");

        if (totalSAC > 30 && totalSpartan == 0) {
            flagAndAlert("sacTotal=" + totalSAC + " spartan=0 (potential Spartan bypass)");
        } else if (totalSpartan > 30 && totalSAC < 10) {
            flagAndAlert("spartan=" + totalSpartan + " sac=" + totalSAC + " (potential SAC bypass)");
        } else {
            reward();
        }
    }

    private int getTotalSACVL() {
        return (int) player.checkManager.allChecks.values().stream()
            .mapToDouble(c -> ((Check)c).violations).sum();
    }
}
