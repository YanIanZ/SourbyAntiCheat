package dev.yanianz.sourbyanticheat.checks.impl.movement;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "InventoryWalk", configName = "inventorywalk", stableKey = "sac.movement.inventorywalk",
    description = "Detects movement while player inventory is open", setback = 10, decay = 0.025)
public class InventoryWalk extends Check implements PacketCheck {

    private boolean inventoryOpen = false;
    private long lastInventoryPacket = 0;
    private static final long INVENTORY_TIMEOUT_MS = 5000;
    private int buffer = 0;

    public InventoryWalk(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW
                || event.getPacketType() == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION
                || event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW
                || event.getPacketType() == PacketType.Play.Client.CLIENT_SETTINGS) {
            lastInventoryPacket = System.currentTimeMillis();
            if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
                inventoryOpen = false;
            }
            return;
        }

        long now = System.currentTimeMillis();
        inventoryOpen = now - lastInventoryPacket < INVENTORY_TIMEOUT_MS;

        if (!inventoryOpen) return;

        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
        if (!flying.hasPositionChanged()) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.inVehicle() || player.isFlying || player.canFly || player.isGliding) return;

        double deltaX = player.x - player.lastX;
        double deltaZ = player.z - player.lastZ;
        double horizontalDist = deltaX * deltaX + deltaZ * deltaZ;

        if (horizontalDist < 0.001) return;

        buffer++;
        if (buffer > 4) {
            flagAndAlert("dist=" + String.format("%.4f", Math.sqrt(horizontalDist)) + " buffer=" + buffer);
        } else {
            reward();
        }
    }
}
