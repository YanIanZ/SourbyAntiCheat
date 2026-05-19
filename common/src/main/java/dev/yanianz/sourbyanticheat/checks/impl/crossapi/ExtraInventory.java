package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
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

    // Config-wired thresholds (defaults equal prior hardcoded values).
    // Player inventory (window 0) has valid slots 0-45: 0-44 are armour/craft/main/hotbar,
    // slot 45 is the offhand. Anything above 45 is an invalid slot index — so the highest
    // legal slot is 45 and the check flags getSlot() > maxSlot.
    private int maxSlot = 45;
    private double nettyRateThreshold = 120.0;

    public ExtraInventory(SacPlayer player) { super(player); }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        maxSlot            = config.getIntElse(base + "max-slot",               45);
        nettyRateThreshold = config.getDoubleElse(base + "netty-rate-threshold", 120.0);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) return;

        WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);
        if (click.getWindowId() == 0 && click.getSlot() > maxSlot) {
            boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > nettyRateThreshold;
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
