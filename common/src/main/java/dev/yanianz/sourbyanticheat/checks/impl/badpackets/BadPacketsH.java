package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.BlockPlaceCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockBreak;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockPlace;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;

@CheckData(name = "BadPacketsH", stableKey = "sac.badpackets.unexpected_sequence", description = "Sent unexpected sequence id", decay = 0.01)
public class BadPacketsH extends BlockPlaceCheck {
    private int lastSequence;
    private boolean acceptNextSequence;
    private final boolean isSupportedVersion = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19) && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_19);

    public BadPacketsH(final SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM
                && shouldCancel(new WrapperPlayClientUseItem(event).getSequence())) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        if (shouldCancel(place.sequence) && shouldCancel()) {
            place.resync();
        }
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        switch (blockBreak.action) {
            case START_DIGGING, FINISHED_DIGGING -> {
                if (shouldCancel(blockBreak.sequence)) {
                    blockBreak.cancel();
                }
            }
            case CANCELLED_DIGGING -> { // other actions will be checked by BadPacketsL
                if (blockBreak.sequence != 0 && flagAndAlert("expected=0, id=" + blockBreak.sequence) && shouldModifyPackets()) {
                    blockBreak.cancel();
                }
            }
        }
    }

    public boolean shouldCancel(int sequence) {
        // Versions below 1.19 have no sequence field — never alert there
        if (!isSupportedVersion) {
            return false;
        }
        // After a world-change the client keeps its own sequence counter, so the
        // first packet establishes the new baseline instead of flagging
        if (acceptNextSequence) {
            acceptNextSequence = false;
            lastSequence = sequence;
            reward();
            return false;
        }
        int expected = lastSequence + 1;
        boolean invalid = sequence != expected;
        if (invalid) {
            flagAndAlert("expected=" + expected + ", id=" + sequence);
        } else {
            lastSequence = sequence;
            reward();
        }
        return invalid && shouldModifyPackets();
    }

    public void onWorldChange() {
        // Do NOT reset to 0 — the client's sequence counter is unaffected by world
        // changes; accept the next sequence as the new baseline instead
        acceptNextSequence = true;
    }
}
