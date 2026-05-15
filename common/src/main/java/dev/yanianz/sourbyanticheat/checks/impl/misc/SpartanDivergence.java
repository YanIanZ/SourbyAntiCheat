package dev.yanianz.sourbyanticheat.checks.impl.misc;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "SpartanDivergence", stableKey = "sac.spartan.divergence", description = "Tracks SAC-Spartan VL divergence over time", setback = 25)
public class SpartanDivergence extends Check implements PacketCheck {

    private int ticks = 0;
    private int divergenceStreak = 0;
    private static final int CHECK_EVERY = 200;

    public SpartanDivergence(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (!SpartanCrossCheck.isAvailable()) return;

        ticks++;
        if (ticks < CHECK_EVERY) return;
        ticks = 0;

        int sacVL = getTotalSACVL();
        int spartaVL = SpartanCrossCheck.getSpartanPerCheckVL(player.uuid, "total");

        int diff = Math.abs(sacVL - spartaVL);
        if (diff > 30) {
            divergenceStreak++;
            if (divergenceStreak > 3) {
                flagAndAlert("sac=" + sacVL + " spartan=" + spartaVL + " diff=" + diff + " streak=" + divergenceStreak);
            }
        } else {
            divergenceStreak = Math.max(0, divergenceStreak - 1);
            reward();
        }
    }

    private int getTotalSACVL() {
        return (int) player.checkManager.allChecks.values().stream()
            .mapToDouble(c -> ((Check) c).violations).sum();
    }
}
