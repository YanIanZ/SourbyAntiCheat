package dev.yanianz.sourbyanticheat.platform.bukkit;

import dev.yanianz.sourbyanticheat.SacAPI;
import dev.yanianz.sourbyanticheat.utils.anticheat.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.UUID;

/**
 * Forwards SAC alerts to connected BungeeCord/Velocity proxy
 * via plugin messaging channel sac:main.
 */
public final class ProxyForwarder {

    private static final String CHANNEL = "sac:main";

    public static void sendAlert(UUID playerUuid, String serverName, String alertJson) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF("Alert");
            out.writeUTF(playerUuid.toString());
            out.writeUTF(serverName);
            out.writeUTF(alertJson);

            Player sender = findOnlinePlayer();
            if (sender != null) {
                sender.sendPluginMessage(
                    (org.bukkit.plugin.Plugin) SacAPI.INSTANCE.getSacPlugin(),
                    CHANNEL, bytes.toByteArray()
                );
            }
        } catch (Exception e) {
            LogUtil.warn("Failed to forward alert to proxy: " + e.getMessage());
        }
    }

    private static Player findOnlinePlayer() {
        try {
            var it = Bukkit.getOnlinePlayers().iterator();
            return it.hasNext() ? it.next() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private ProxyForwarder() {}
}
