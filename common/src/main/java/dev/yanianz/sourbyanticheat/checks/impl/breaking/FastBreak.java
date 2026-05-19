package dev.yanianz.sourbyanticheat.checks.impl.breaking;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.BlockBreakCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockBreak;
import dev.yanianz.sourbyanticheat.utils.math.SacMath;
import dev.yanianz.sourbyanticheat.utils.nmsutil.BlockBreakSpeed;
import dev.yanianz.sourbyanticheat.utils.viaversion.ViaVersionUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

import java.util.Set;

// Based loosely off of Hawk BlockBreakSpeedSurvival
// Also based loosely off of NoCheatPlus FastBreak
// Also based off minecraft wiki: https://minecraft.wiki/w/Breaking#Instant_breaking
@CheckData(name = "FastBreak", stableKey = "sac.breaking.fast_break", description = "Breaking blocks too quickly")
public class FastBreak extends Check implements BlockBreakCheck {

    // For some reason these states flag and I don't know why.
    // Better to just exempt to not annoy legit players.
    private static final Set<StateType> EXEMPT_STATES = Set.of();
    private final boolean clientOlderThanServer = PacketEvents.getAPI().getServerManager().getVersion().getProtocolVersion() > player.getClientVersion().getProtocolVersion();

    // Config-wired thresholds (defaults equal prior hardcoded values).
    private long balanceFlagMs = 1000;      // break-balance threshold to flag
    private long closeEnoughDiffMs = 25;    // FINISHED diff < this -> reduce break balance
    private long startBreakGraceMs = 50;    // grace applied to the START_DIGGING timestamp

    public FastBreak(SacPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.balanceFlagMs      = config.getIntElse(base + "balance-flag-ms", 1000);
        this.closeEnoughDiffMs  = config.getIntElse(base + "close-enough-diff-ms", 25);
        this.startBreakGraceMs  = config.getIntElse(base + "start-break-grace-ms", 50);
    }

    // The block the player is currently breaking
    Vector3i targetBlockPosition = null;
    // The maximum amount of damage the player deals to the block
    double maximumBlockDamage = 0;
    // The time the player started to break the block (nanoTime)
    long startBreak = 0;

    // The buffer to this check
    double blockBreakBalance = 0;

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        // Creative breaking is instant by design — predicted break time has nothing to
        // measure against, so the balance would climb on every legitimate creative break.
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) {
            return;
        }

        // Lag-spike exemption: when the server cannot process the player's ticks
        // reliably the elapsed-time measurement is unreliable — abort the in-progress
        // measurement rather than measure a (falsely) fast break.
        if (player.canSkipTicks()) {
            targetBlockPosition = null;
            return;
        }

        // An aborted dig (switched target / released the button before the block broke)
        // is not a fast break. Clear the in-progress state so a later FINISHED packet
        // cannot measure against a stale block or timestamp.
        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) {
            targetBlockPosition = null;
            reward();
            return;
        }

        if (blockBreak.action == DiggingAction.START_DIGGING) {
            if (!ViaVersionUtil.isAvailable) {
                // Exempt all blocks that do not exist in the player version
                final WrappedBlockState defaultState = WrappedBlockState.getDefaultState(player.getClientVersion(), blockBreak.block.getType());
                if (defaultState.getType() == StateTypes.AIR || EXEMPT_STATES.contains(defaultState.getType())) {
                    return;
                }
            }
            // If client is older than the server, fetch block client actually sees from via
            // otherwise just return the server-side block (since if client is >= server version the block is guaranteed to exist in client version)
            WrappedBlockState block = clientOlderThanServer ? WrappedBlockState.getByGlobalId(player.getClientVersion(), player.getViaTranslatedClientBlockID(blockBreak.block.getGlobalId())) : blockBreak.block;

            // Deterministic grace: always credit the player the START_DIGGING grace window.
            // It compensates for the gap between the client deciding to dig and the
            // packet reaching the server, so the predicted break time isn't measured short.
            startBreak = System.nanoTime() - msToNanos(startBreakGraceMs);
            targetBlockPosition = blockBreak.position;
            maximumBlockDamage = BlockBreakSpeed.getBlockDamage(player, block);
            // No flag on START — switching break targets or fast mining rhythm is not a
            // cheat. Detection happens only on FINISHED_DIGGING, against the block's
            // own physically-predicted break time.
        }

        if (blockBreak.action == DiggingAction.FINISHED_DIGGING && targetBlockPosition != null) {
            double predictedTime = Math.ceil(1 / maximumBlockDamage) * 50;
            double realTime = nanosToMs(System.nanoTime() - startBreak);
            double diff = predictedTime - realTime;

            clampBalance();

            if (diff < closeEnoughDiffMs) {  // Reduce buffer if "close enough"
                blockBreakBalance *= 0.9;
            } else { // Broke faster than physically possible — increase buffer
                blockBreakBalance += diff;
            }

            if (blockBreakBalance > balanceFlagMs) { // More than a second of advantage
                if (flagAndAlert("diff=" + diff + "ms, balance=" + blockBreakBalance + "ms, type=" + blockBreak.block.getType()) && shouldModifyPackets()) {
                    blockBreak.cancel();
                }
            } else {
                reward();
            }

            // Break finished — clear the in-progress state.
            targetBlockPosition = null;
        }
    }

    private static long msToNanos(long ms) {
        return ms * 1_000_000L;
    }

    private static double nanosToMs(long nanos) {
        return nanos / 1_000_000.0;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Find the most optimal block damage using the animation packet, which is sent at least once a tick when breaking blocks
        // On 1.8 clients, via screws with this packet meaning we must fall back to the 1.8 idle flying packet
        if ((player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? event.getPacketType() == PacketType.Play.Client.ANIMATION : WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) && targetBlockPosition != null) {
            maximumBlockDamage = Math.max(maximumBlockDamage, BlockBreakSpeed.getBlockDamage(player, player.compensatedWorld.getBlock(targetBlockPosition)));
        }
    }

    private void clampBalance() {
        double balance = Math.max(1000, (player.getTransactionPing()));
        blockBreakBalance = SacMath.clamp(blockBreakBalance, -balance, balance);
    }
}
