package dev.yanianz.sourbyanticheat.checks.impl.movement;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "Blink", stableKey = "sac.movement.blink", description = "Detects blink/fakelag hacks", setback = 15, decay = 0.02)
public class Blink extends Check implements PacketCheck {

    private long lastPacketTime = 0;
    private int blinkCount = 0;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private long gapThresholdMs = 500;
    private int blinkCountThreshold = 5;
    private int decrement = 1;

    public Blink(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.gapThresholdMs = config.getIntElse(base + "gap-threshold-ms", 500);
        this.blinkCountThreshold = config.getIntElse(base + "blink-count-threshold", 5);
        this.decrement = config.getIntElse(base + "decrement", 1);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;
        if (player.packetStateData.lastPacketWasTeleport) return;

        long now = System.currentTimeMillis();
        if (lastPacketTime > 0) {
            long gap = now - lastPacketTime;
            // Server-lag/GC/packet-batching exemption — if the server itself failed to tick
            // reliably the gap is explained by server-side stalls, not a blink hack.
            boolean serverLagged = !player.isTickingReliablyFor(3);
            if (gap > gapThresholdMs && !serverLagged) {
                blinkCount++;
                if (blinkCount > blinkCountThreshold) {
                    flagAndAlert("gap=" + gap + "ms count=" + blinkCount);
                }
            } else {
                // Normal-gap packet ends the suspicious condition — reset, decay, and reward.
                blinkCount = Math.max(0, blinkCount - decrement);
                reward();
            }
        }
        lastPacketTime = now;
    }
}
