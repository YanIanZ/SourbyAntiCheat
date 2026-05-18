package dev.yanianz.sourbyanticheat.checks.impl.movement;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "InventoryWalk", configName = "inventorywalk", stableKey = "sac.movement.inventorywalk",
    description = "Detects movement while player inventory is open", setback = 10, decay = 0.025)
public class InventoryWalk extends Check implements PacketCheck {

    // Reflects last tick's open state — decided from lastInventoryPacket BEFORE this tick's
    // processing, so the set+use no longer happens within one invocation.
    private boolean inventoryOpen = false;
    private long lastInventoryPacket = 0;
    // Timestamp of the most recent inventory close — used for closing-with-momentum grace.
    private long lastInventoryClose = 0;
    private int buffer = 0;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private long inventoryTimeoutMs = 3000;
    private double moveThreshold = 0.001;
    private int bufferThreshold = 6;
    // Grace after a close: a player still carrying walking momentum is not cheating.
    private static final long CLOSE_GRACE_MS = 1000;

    public InventoryWalk(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.inventoryTimeoutMs = config.getIntElse(base + "inventory-timeout-ms", 3000);
        this.moveThreshold = config.getDoubleElse(base + "move-threshold", 0.001);
        this.bufferThreshold = config.getIntElse(base + "buffer-threshold", 6);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.compensatedEntities.self.isDead) return;

        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);
            if (click.getWindowId() == 0) {
                lastInventoryPacket = System.currentTimeMillis();
            }
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
            lastInventoryPacket = System.currentTimeMillis();
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            lastInventoryClose = System.currentTimeMillis();
            inventoryOpen = false;
            return;
        }

        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) return;

        WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
        if (!flying.hasPositionChanged()) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.inVehicle() || player.isFlying || player.canFly || player.isGliding) return;

        long now = System.currentTimeMillis();

        // Act on LAST tick's open state, then update it for the next tick — this defers the
        // set+use by one invocation so a freshly-registered open does not flag the same tick.
        boolean wasOpen = inventoryOpen;
        // Open-screen state: any recent inventory interaction OR a server-opened inventory.
        inventoryOpen = (now - lastInventoryPacket < inventoryTimeoutMs)
                || player.serverOpenedInventoryThisTick;

        if (!wasOpen) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        // Closing-with-momentum grace — a just-closed inventory leaves residual walk velocity.
        if (now - lastInventoryClose < CLOSE_GRACE_MS) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        double deltaX = player.x - player.lastX;
        double deltaZ = player.z - player.lastZ;
        double horizontalDist = deltaX * deltaX + deltaZ * deltaZ;

        if (horizontalDist < moveThreshold) {
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        buffer++;
        if (buffer > bufferThreshold) {
            flagAndAlert("dist=" + String.format("%.4f", Math.sqrt(horizontalDist)) + " buffer=" + buffer);
        } else {
            reward();
        }
    }
}
