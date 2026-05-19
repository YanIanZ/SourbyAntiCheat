package dev.yanianz.sourbyanticheat.checks.impl.breaking;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.BlockBreakCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.MessageUtil;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;

import static dev.yanianz.sourbyanticheat.utils.nmsutil.BlockBreakSpeed.getBlockDamage;

@CheckData(name = "WrongBreak", stableKey = "sac.breaking.wrong_break")
public class WrongBreak extends Check implements BlockBreakCheck {
    // The sentinel Y position the legacy "weird cancel" packet carries: pre-1.8 clients use 255,
    // 1.14+ servers report -1, otherwise 4095. (Not an exempted Y coordinate — it is a marker value.)
    private final int legacyCancelYSentinel = player.getClientVersion().isOlderThan(ClientVersion.V_1_8) ? 255 : (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_14) ? -1 : 4095);
    private boolean lastBlockWasInstantBreak = false;
    private boolean lastActionWasCancel = false;
    private Vector3i lastBlock, lastCancelledBlock, lastLastBlock = null;

    public WrongBreak(final SacPlayer player) {
        super(player);
    }

    // The client sometimes sends a wierd cancel packet
    private boolean shouldExempt(final WrappedBlockState block, int yPos) {
        // The weird-cancel signature is a single cancel right after a start. A cancel that
        // directly follows another cancel is a genuine 2nd cancel — it must not be exempted.
        if (lastActionWasCancel)
            return false;

        // lastLastBlock is always null when this happens, and lastBlock isn't
        if (lastLastBlock != null || lastBlock == null)
            return false;

        // on pre 1.14.4 clients, the YPos of this packet is always the same
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4) && yPos != legacyCancelYSentinel)
            return false;

        // and if this block is not an instant break
        return player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4) || getBlockDamage(player, block) < 1;
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        // A teleport invalidates the tracked dig state — clear it so a stale block does not flag.
        if (player.packetStateData.lastPacketWasTeleport) {
            lastBlock = null;
            lastLastBlock = null;
            lastCancelledBlock = null;
            lastActionWasCancel = false;
        }

        if (blockBreak.action == DiggingAction.START_DIGGING) {
            final Vector3i pos = blockBreak.position;

            lastBlockWasInstantBreak = getBlockDamage(player, blockBreak.block) >= 1;
            lastCancelledBlock = null;
            lastLastBlock = lastBlock;
            lastBlock = pos;
            lastActionWasCancel = false;
        }

        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) {
            final Vector3i pos = blockBreak.position;

            boolean flagged = false;
            if (!shouldExempt(blockBreak.block, pos.y) && !pos.equals(lastBlock)) {
                // https://github.com/GrimAnticheat/Grim/issues/1512
                if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4) || (!lastBlockWasInstantBreak && pos.equals(lastCancelledBlock))) {
                    if (flagAndAlert("action=CANCELLED_DIGGING" + ", last=" + MessageUtil.toUnlabledString(lastBlock) + ", pos=" + MessageUtil.toUnlabledString(pos))) {
                        flagged = true;
                        if (shouldModifyPackets()) {
                            blockBreak.cancel();
                        }
                    }
                }
            }
            if (!flagged) {
                reward();
            }

            lastCancelledBlock = pos;
            lastLastBlock = null;
            lastBlock = null;
            lastActionWasCancel = true;
            return;
        }

        if (blockBreak.action == DiggingAction.FINISHED_DIGGING) {
            final Vector3i pos = blockBreak.position;

            boolean flagged = false;
            // when a player looks away from the mined block, they send a cancel, and if they look at it again, they don't send another start. (thanks mojang!)
            if (!pos.equals(lastCancelledBlock) && (!lastBlockWasInstantBreak || player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4)) && !pos.equals(lastBlock)) {
                if (flagAndAlert("action=FINISHED_DIGGING" + ", last=" + MessageUtil.toUnlabledString(lastBlock) + ", pos=" + MessageUtil.toUnlabledString(pos))) {
                    flagged = true;
                    if (shouldModifyPackets()) {
                        blockBreak.cancel();
                    }
                }
            }
            if (!flagged) {
                reward();
            }

            lastActionWasCancel = false;

            // 1.14.4+ clients don't send another start break in protected regions
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4)) {
                lastCancelledBlock = null;
                lastLastBlock = null;
                lastBlock = null;
            }
        }
    }
}
