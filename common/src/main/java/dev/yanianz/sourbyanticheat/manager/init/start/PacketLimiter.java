package dev.yanianz.sourbyanticheat.manager.init.start;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.player.SacPlayer;

public class PacketLimiter implements StartableInitable {
    @Override
    public void start() {
        SacAPI.INSTANCE.getScheduler().getAsyncScheduler().runAtFixedRate(SacAPI.INSTANCE.getGrimPlugin(), () -> {
            for (SacPlayer player : SacAPI.INSTANCE.getPlayerDataManager().getEntries()) {
                // Avoid concurrent reading on an integer as it's results are unknown
                player.cancelledPackets.set(0);
            }
        }, 1, 20);
    }
}
