package dev.yanianz.sourbyanticheat.manager.init.stop;

import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.PacketEvents;

public class TerminatePacketEvents implements StoppableInitable {
    @Override
    public void stop() {
        LogUtil.info("Terminating PacketEvents...");
        PacketEvents.getAPI().terminate();
    }
}
