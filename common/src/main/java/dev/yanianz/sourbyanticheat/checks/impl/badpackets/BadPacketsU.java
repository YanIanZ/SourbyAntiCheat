package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;

@CheckData(name = "BadPacketsU", stableKey = "sac.badpackets.invalid_block_placement", description = "Sent impossible use item packet")
public class BadPacketsU extends Check implements PacketCheck {

    // The USE_ITEM placement packet is always sent at (-1, -1, -1); the y component is
    // wrapped to this protocol-defined value (differs between modern and legacy clients).
    private static final int EXPECTED_Y_MODERN = 4095;
    private static final int EXPECTED_Y_LEGACY = 255;

    public BadPacketsU(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            final WrapperPlayClientPlayerBlockPlacement packet = new WrapperPlayClientPlayerBlockPlacement(event);
            // BlockFace.OTHER is USE_ITEM for pre 1.9
            if (packet.getFace() == BlockFace.OTHER) {

                // This packet is always sent at (-1, -1, -1) at (0, 0, 0) on the block
                // except y gets wrapped?
                final int expectedY = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8) ? EXPECTED_Y_MODERN : EXPECTED_Y_LEGACY;

                final boolean failedItemCheck = packet.getItemStack().isPresent() && isEmpty(packet.getItemStack().get())
                        // ViaVersion can sometimes cause this part of the check to false
                        && player.getClientVersion().isOlderThan(ClientVersion.V_1_9);

                final Vector3i pos = packet.getBlockPosition();
                final Vector3f cursor = packet.getCursorPosition();

                if (failedItemCheck
                        || pos.x != -1
                        || pos.y != expectedY
                        || pos.z != -1
                        || cursor.x != 0
                        || cursor.y != 0
                        || cursor.z != 0
                        || packet.getSequence() != 0
                ) {
                    final String verbose = String.format(
                            "xyz=%s, %s, %s, cursor=%s, %s, %s, item=%s, sequence=%s",
                            pos.x, pos.y, pos.z, cursor.x, cursor.y, cursor.z, !failedItemCheck, packet.getSequence()
                    );
                    if (flagAndAlert(verbose) && shouldModifyPackets()) {
                        player.onPacketCancel();
                        event.setCancelled(true);
                    }
                } else {
                    reward();
                }
            }
        }
    }

    private boolean isEmpty(ItemStack itemStack) {
        return itemStack.getType() == null || itemStack.getType() == ItemTypes.AIR;
    }
}
