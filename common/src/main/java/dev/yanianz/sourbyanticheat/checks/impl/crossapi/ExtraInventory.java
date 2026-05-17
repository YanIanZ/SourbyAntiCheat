package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "ExtraInventory", configName = "extrainventory", decay = 0.02, setback = 5, stableKey = "cross.extrainventory")
public class ExtraInventory extends Check implements PacketCheck {

    private int buffer;
    private static final int MAX_SLOT = 45;
    private static final double NETTY_RATE_THRESHOLD = 15.0;

    public ExtraInventory(SacPlayer player) { super(player); }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) return;

        WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);
        if (click.getWindowId() == 0 && click.getSlot() > MAX_SLOT) {
            boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;
            SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "Exploits");
            boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

            buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
            if (buffer > 2) {
                flagAndAlert(String.format("slot=%d netty=%.1f/s spartan=%s",
                    click.getSlot(), player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
            }
        } else {
            buffer = Math.max(0, buffer - 1);
            reward();
        }
    }
}
