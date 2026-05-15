package dev.yanianz.sourbyanticheat.checks.impl.misc;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "APIBypass", stableKey = "sac.api.bypass", description = "Detects when external plugins disable too many checks via API", setback = 0)
public class APIBypass extends Check implements PacketCheck {

    private int lastDisabledCount = 0;
    private int warningStreak = 0;

    public APIBypass(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        long disabledCount = player.checkManager.allChecks.values().stream()
            .filter(c -> c instanceof Check && !((Check) c).isEnabled()).count();
        long totalCount = player.checkManager.allChecks.size();

        if (totalCount == 0) return;
        double ratio = (double) disabledCount / totalCount;

        if (ratio > 0.5 && disabledCount != lastDisabledCount) {
            warningStreak++;
            if (warningStreak > 5) {
                flagAndAlert("disabled=" + disabledCount + "/" + totalCount + " (" + String.format("%.0f%%", ratio * 100) + ")");
            }
        } else {
            warningStreak = Math.max(0, warningStreak - 1);
        }
        lastDisabledCount = (int) disabledCount;
    }
}
