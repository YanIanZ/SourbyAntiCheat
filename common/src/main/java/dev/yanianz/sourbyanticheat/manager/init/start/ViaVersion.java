package dev.yanianz.sourbyanticheat.manager.init.start;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import dev.yanianz.sourbyanticheat.utils.viaversion.ViaVersionUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.viaversion.viaversion.api.Via;

public class ViaVersion implements StartableInitable {

    @Override
    public void start() {
        if (!ViaVersionUtil.isAvailable) return;
        ViaVersionUtil.injectHooks();

        ServerVersion serverVersion = PacketEvents.getAPI().getServerManager().getVersion();

        if (Via.getConfig().getValues().containsKey("fix-1_21-placement-rotation") && Via.getConfig().fix1_21PlacementRotation() && serverVersion.isOlderThan(ServerVersion.V_1_21)) {
            LogUtil.error("Sac has detected that you are using ViaVersion with the `fix-1_21-placement-rotation` option enabled.");
            LogUtil.error("This option is known to cause issues with Sac and may result in false positives and bypasses.");
            LogUtil.error("Please disable this option in your ViaVersion configuration to prevent these issues.");
        }

        if (SacAPI.INSTANCE.getPluginManager().getPlugin("ViaBackwards") != null && serverVersion.isNewerThanOrEquals(ServerVersion.V_1_21_2)) {
            LogUtil.warn("Sac has detected that you have installed ViaBackwards on a 1.21.2+ server.");
            LogUtil.warn("This setup is currently unsupported and you will experience issues with older clients using vehicles.");
        }
    }
}
