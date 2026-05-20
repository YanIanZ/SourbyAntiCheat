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
            LogUtil.error("ViaVersion `fix-1_21-placement-rotation` is enabled on a <1.21 server.");
            LogUtil.error("This option is known to cause issues with Sac and may result in false positives and bypasses.");
            LogUtil.error("Please disable this option in your ViaVersion configuration to prevent these issues.");
        }

        if (SacAPI.INSTANCE.getPluginManager().getPlugin("ViaBackwards") != null && serverVersion.isNewerThanOrEquals(ServerVersion.V_1_21_2)) {
            LogUtil.warn("ViaBackwards detected on a 1.21.2+ server.");
            LogUtil.warn("This setup is currently unsupported — older clients using vehicles may experience issues.");
        }

        if (SacAPI.INSTANCE.getPluginManager().getPlugin("ViaRewind") != null && serverVersion.isNewerThanOrEquals(ServerVersion.V_1_9)) {
            LogUtil.warn("ViaRewind detected on a 1.9+ server.");
            LogUtil.warn("1.8 clients translated through ViaRewind may have packet-ordering issues. SAC applies known workarounds but some edge cases may remain.");
        }
    }
}
