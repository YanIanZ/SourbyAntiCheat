package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "AutoRespawn", configName = "autorespawn", decay = 0.02, setback = 5, stableKey = "cross.autorespawn")
public class AutoRespawn extends Check implements PacketCheck {

    private int buffer;
    private long lastDeathTime = 0;
    private static final double NETTY_RATE_THRESHOLD = 15.0;
    private long gapThreshold = 500;

    public AutoRespawn(SacPlayer player) { super(player); }

    @Override
    public void onReload(ConfigManager config) {
        this.gapThreshold = config.getIntElse(getConfigName() + ".gap-threshold-ms", 500);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        if (event.getPacketType() == PacketType.Play.Client.CLIENT_STATUS) {
            var status = new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus(event);
            if (status.getAction() == com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus.Action.PERFORM_RESPAWN) {
                long now = System.currentTimeMillis();
                if (lastDeathTime > 0) {
                    long gap = now - lastDeathTime;

                    // Plugin-forced / server-initiated respawn exemption: server may immediately re-trigger respawn
                    if (gap <= 0) {
                        lastDeathTime = now;
                        reward();
                        return;
                    }

                    if (gap < gapThreshold) {
                        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;
                        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "Exploits");
                        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;
                        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
                        if (buffer > 2) {
                            flagAndAlert(String.format("respawnGap=%dms netty=%.1f/s spartan=%s",
                                gap, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
                            lastDeathTime = now;
                            return;
                        }
                    } else {
                        buffer = Math.max(0, buffer - 1);
                        reward();
                    }
                }
                lastDeathTime = now;
            }
        }
    }
}
