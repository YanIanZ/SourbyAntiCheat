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
    // Timing windows are stored in nanoseconds; the *Ms fields keep the documented ms value.
    private long delayRewardMs = 275;       // breakDelay >= this -> reduce delay balance
    private long delayPenaltyBaseMs = 300;  // penalty = this - breakDelay
    private long balanceFlagMs = 1000;      // balance threshold to flag
    private long closeEnoughDiffMs = 25;    // FINISHED diff < this -> reduce break balance
    private long startBreakGraceMs = 50;    // grace applied to the first START_DIGGING

    public FastBreak(SacPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.delayRewardMs      = config.getIntElse(base + "delay-reward-ms", 275);
        this.delayPenaltyBaseMs = config.getIntElse(base + "delay-penalty-base-ms", 300);
        this.balanceFlagMs      = config.getIntElse(base + "balance-flag-ms", 1000);
        this.closeEnoughDiffMs  = config.getIntElse(base + "close-enough-diff-ms", 25);
        this.startBreakGraceMs  = config.getIntElse(base + "start-break-grace-ms", 50);
    }

    // The block the player is currently breaking
    Vector3i targetBlockPosition = null;
    // The maximum amount of damage the player deals to the block
    //
    double maximumBlockDamage = 0;
    // The last time a finish digging packet was sent (nanoTime), to enforce delay after non-instabreak
    long lastFinishBreak = 0;
    // The time the player started to break the block (nanoTime)
    long startBreak = 0;
    // nanoTime()'s origin is arbitrary (and may be negative), so a raw 0-initialized
    // baseline would produce a bogus delay/diff on the very first break. Treat the
    // first break as clean and only establish the baseline timestamps.
    boolean firstBreak = true;

    // The buffer to this check
    double blockBreakBalance = 0;
    double blockDelayBalance = 0;

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        // Creative breaking is instant by design — predicted break time has nothing to
        // measure against, so the balance would climb on every legitimate creative break.
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) {
            return;
        }

        // Lag-spike exemption beyond the ping clamp: when the server cannot process the
        // player's ticks reliably, the elapsed-time measurement is unreliable, so reset
        // timing state instead of measuring a (falsely) fast break.
        if (player.canSkipTicks()) {
            long now = System.nanoTime();
            startBreak = now;
            lastFinishBreak = now;
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
            // TODO this lazy loads PacketEvents mappings for older versions for clients on versions older than the servers, increasing memory usage
            //  * its the only thing we use non-native mappings for behind ViaVersion
            //  * can we translate back "up" to server version and run check against server version to avoid loading older registries?
            WrappedBlockState block = clientOlderThanServer ? WrappedBlockState.getByGlobalId(player.getClientVersion(), player.getViaTranslatedClientBlockID(blockBreak.block.getGlobalId())) : blockBreak.block;

            // Deterministic grace: always credit the player the START_DIGGING grace window.
            // It compensates for the gap between the client deciding to dig and the
            // packet reaching the server, so the predicted break time isn't measured short.
            startBreak = System.nanoTime() - msToNanos(startBreakGraceMs);
            targetBlockPosition = blockBreak.position;

            maximumBlockDamage = BlockBreakSpeed.getBlockDamage(player, block);

            // First break: lastFinishBreak has never been set to a real nanoTime
            // value, so skip the delay computation and treat it as clean.
            if (firstBreak) {
                reward();
            } else {
                double breakDelay = nanosToMs(System.nanoTime() - lastFinishBreak);

                if (breakDelay >= delayRewardMs) { // Reduce buffer if "close enough"
                    blockDelayBalance *= 0.9;
                } else { // Otherwise, increase buffer
                    blockDelayBalance += delayPenaltyBaseMs - breakDelay;
                }

                if (blockDelayBalance > balanceFlagMs) { // If more than a second of advantage
                    if (flagAndAlert("delay=" + breakDelay + "ms, type=" + blockBreak.block.getType()) && shouldModifyPackets()) {
                        blockBreak.cancel();
                    }
                } else {
                    reward();
                }
            }

            clampBalance();
        }

        if (blockBreak.action == DiggingAction.FINISHED_DIGGING && targetBlockPosition != null) {
            double predictedTime = Math.ceil(1 / maximumBlockDamage) * 50;
            double realTime = nanosToMs(System.nanoTime() - startBreak);
            double diff = predictedTime - realTime;

            clampBalance();

            if (diff < closeEnoughDiffMs) {  // Reduce buffer if "close enough"
                blockBreakBalance *= 0.9;
            } else { // Otherwise, increase buffer
                blockBreakBalance += diff;
            }

            if (blockBreakBalance > balanceFlagMs) { // If more than a second of advantage
                if (flagAndAlert("diff=" + diff + "ms, balance=" + blockBreakBalance + "ms, type=" + blockBreak.block.getType()) && shouldModifyPackets()) {
                    blockBreak.cancel();
                }
            } else {
                reward();
            }

            // also set start time because the breaking netcode is fucked on 1.14.4+
            lastFinishBreak = startBreak = System.nanoTime();
            // Baseline established; subsequent breaks measure real delay.
            firstBreak = false;
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
        blockBreakBalance = SacMath.clamp(blockBreakBalance, -balance, balance); // Clamp not Math.max in case other logic changes
        blockDelayBalance = SacMath.clamp(blockDelayBalance, -balance, balance);
    }
}
