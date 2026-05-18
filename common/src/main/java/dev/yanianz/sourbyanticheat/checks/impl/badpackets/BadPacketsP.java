package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow.WindowClickType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;

@CheckData(name = "BadPacketsP", stableKey = "sac.badpackets.invalid_click", description = "Invalid window click packet")
public class BadPacketsP extends Check implements PacketCheck {

    private int containerType = -1;
    private int containerId = -1;

    // Config-wired button bounds (defaults equal prior hardcoded vanilla-protocol values)
    private int maxPickupButton  = 2;
    private int maxSwapButton    = 8;
    private int swapOffhandButton = 40;
    private int maxQuickCraftButton = 10;

    public BadPacketsP(SacPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        maxPickupButton     = config.getIntElse(base + "max-pickup-button", 2);
        maxSwapButton       = config.getIntElse(base + "max-swap-button", 8);
        swapOffhandButton   = config.getIntElse(base + "swap-offhand-button", 40);
        maxQuickCraftButton = config.getIntElse(base + "max-quick-craft-button", 10);
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            WrapperPlayServerOpenWindow window = new WrapperPlayServerOpenWindow(event);
            this.containerType = window.getType();
            this.containerId = window.getContainerId();
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow wrapper = new WrapperPlayClientClickWindow(event);
            WindowClickType clickType = wrapper.getWindowClickType();
            int button = wrapper.getButton();

            // NOTE: bounds below are container-agnostic. They cover the full vanilla
            // protocol range for each click type, so they hold for every container
            // type and never false-positive; per-container slot-count tightening would
            // only add detections, not correctness, and is left as a future enhancement.
            boolean flag = switch (clickType) {
                case PICKUP, QUICK_MOVE, CLONE -> button > maxPickupButton || button < 0;
                case SWAP -> (button > maxSwapButton || button < 0) && button != swapOffhandButton;
                case THROW -> button != 0 && button != 1;
                case QUICK_CRAFT -> button == 3 || button == 7 || button > maxQuickCraftButton || button < 0;
                case PICKUP_ALL -> button != 0;
                case UNKNOWN -> true;
            };

            if (flag) {
                if (flagAndAlert("clickType=" + clickType.toString().toLowerCase() + ", button=" + button + (wrapper.getWindowId() == containerId ? ", container=" + containerType : "")) && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            } else {
                reward();
            }
        }
    }
}
