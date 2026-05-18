package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.BlockBreakCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockBreak;

@CheckData(name = "CrossFastBreak", configName = "crossfastbreak", decay = 0.02, setback = 10, stableKey = "cross.fastbreak")
public class CrossFastBreak extends Check implements BlockBreakCheck {

    private int breakCount;
    private long lastReset;
    private int buffer;

    private int breakThreshold = 8;

    public CrossFastBreak(SacPlayer player) {
        super(player);
        lastReset = System.currentTimeMillis();
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.breakThreshold = config.getIntElse(base + "break-threshold", 8);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        // Break RATE is meaningless in creative (every break is instant) — exempt.
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE) return;
        if (player.compensatedEntities.self.isDead) return;

        long now = System.currentTimeMillis();
        if (now - lastReset > 1000) {
            breakCount = 0;
            lastReset = now;
        }
        breakCount++;

        if (breakCount < breakThreshold) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        // Raw break-rate is unreliable on its own — instant-break blocks (crops, torches,
        // soft blocks with an efficient tool) let a legitimate miner exceed the threshold.
        // Only alert when Spartan independently confirms fast-break.
        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "FastBreak");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        if (spartanConfirms) {
            buffer++;
            if (buffer > 3) {
                flagAndAlert(String.format("breaks=%d/s spartan=%s", breakCount, spartanResult.type()));
                return;
            }
        } else {
            buffer = Math.max(0, buffer - 1);
        }
        reward();
    }
}
