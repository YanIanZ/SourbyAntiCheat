package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsE", stableKey = "sac.badpackets.invalid_position")
public class BadPacketsE extends Check implements PacketCheck {
    private int noReminderTicks;
    // default = prior hardcoded value (20 for <=1.8 clients, 19 otherwise)
    private int maxNoReminderTicks = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8) ? 20 : 19;
    private final boolean isViaPleaseStopUsingProtocolHacksOnYourServer = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2) || PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21_2);

    public BadPacketsE(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        int defaultMax = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8) ? 20 : 19;
        this.maxNoReminderTicks = config.getIntElse(getConfigName() + ".max-no-reminder-ticks", defaultMax);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION ||
                event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) {
            noReminderTicks = 0;
        } else if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) && !player.packetStateData.lastPacketWasTeleport) {
            if (++noReminderTicks > maxNoReminderTicks) {
                flagAndAlert("ticks=" + noReminderTicks);
                // Reset to threshold so we flag once per over-threshold episode
                // rather than every flying packet; a cheat genuinely withholding
                // position packets will re-accumulate and flag again.
                noReminderTicks = maxNoReminderTicks;
            } else {
                reward();
            }
        } else if (event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE
                || (isViaPleaseStopUsingProtocolHacksOnYourServer && player.inVehicle())) {
            noReminderTicks = 0; // Exempt vehicles
        }
    }

    public void handleRespawn() {
        noReminderTicks = 0;
    }
}
