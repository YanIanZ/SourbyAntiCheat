package dev.yanianz.sourbyanticheat.checks;

import dev.yanianz.sourbyanticheat.SacAPI;
import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.event.events.FlagEvent;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.profile.Profile;
import dev.yanianz.sourbyanticheat.profile.ProfileAwareConfigView;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.spartan.SpartanEventBridge;
import dev.yanianz.sourbyanticheat.manager.AutoPunishment;
import dev.yanianz.sourbyanticheat.manager.CheckPerformance;
import dev.yanianz.sourbyanticheat.utils.reflection.GeyserUtil;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying;

// Class from https://github.com/Tecnio/AntiCheatBase/blob/master/src/main/java/me/tecnio/anticheat/check/Check.java
@Getter
public class Check extends SacProcessor implements AbstractCheck {
    private static final FlagEvent.Channel FLAG_CHANNEL = SacAPI.INSTANCE.getEventBus().get(FlagEvent.class);

    protected @NotNull final SacPlayer player;

    public double violations;
    private double decay;
    private double setbackVL;
    private double maxVL = -1;
    private int punishWarnVL = -1;
    private int punishKickVL = -1;
    private int punishBanVL = -1;

    private String checkName;
    private String configName;
    private String alternativeName;
    private String displayName;
    private String description;
    private String stableKey = "";

    private @Setter boolean isEnabled;

    public boolean isExperimental() {
        return false;
    }

    private boolean exemptPermission;
    private boolean noSetbackPermission;
    private boolean noModifyPacketPermission;
    private long lastViolationTime;
    private String spartanSuffix = "";
    protected boolean skipForBedrock = true;

    public Check(final @NotNull SacPlayer player) {
        this.player = Objects.requireNonNull(player);

        final CheckData checkData = this.getClass().getAnnotation(CheckData.class);
        if (checkData != null) {
            this.checkName = checkData.name();
            this.configName = checkData.configName();
            // Fall back to check name
            if (this.configName.equals("DEFAULT")) this.configName = this.checkName;
            this.decay = checkData.decay();
            this.setbackVL = checkData.setback();
            this.alternativeName = checkData.alternativeName();
            this.alternativeName = checkData.alternativeName();
            this.description = checkData.description();
            this.stableKey = checkData.stableKey();
            this.displayName = this.checkName;
        }

        reload();
        applyGlobalConfig();
    }

    private void applyGlobalConfig() {
        try {
            var cfg = SacAPI.INSTANCE.getConfigManager().getConfig();
            isEnabled = cfg.getBooleanElse("checks.enabled." + configName, true);
        } catch (Exception ignored) {}
    }

    public boolean shouldModifyPackets() {
        return isEnabled
                && !player.disableGrim
                && !player.noModifyPacketPermission
                && !noModifyPacketPermission
                && !exemptPermission;
    }

    public final void updatePermissions() {
        if (configName == null || player.platformPlayer == null) return;
        final String id = configName.toLowerCase();
        exemptPermission = player.platformPlayer.hasPermission("sac.exempt." + id);
        noSetbackPermission = player.platformPlayer.hasPermission("sac.nosetback." + id);
        noModifyPacketPermission = player.platformPlayer.hasPermission("sac.nomodifypacket." + id);
    }

    public final boolean flagAndAlert(String verbose) {
        if (flag(verbose)) {
            alert(verbose);
            return true;
        }
        return false;
    }

    public final boolean flagAndAlert() {
        return flagAndAlert("");
    }

    public final boolean flag() {
        return flag("");
    }

    public final boolean flag(String verbose) {
        long start = System.nanoTime();
        if (player.disableGrim || exemptPermission || !isEnabled)
            return false;

        try {
            var tracker = SacAPI.INSTANCE.getLeniencyTracker();
            if (tracker != null && tracker.active(checkName, player.uuid)) return false;
        } catch (Throwable ignored) {}

        if (skipForBedrock && GeyserUtil.isBedrockPlayer(player.uuid))
            return false; // §I.7 Bedrock players skip movement/combat checks

        if (FLAG_CHANNEL.fire(player, this, verbose)) {
            CheckPerformance.record(checkName, System.nanoTime() - start);
            return false;
        }

        var spartan = violations >= SpartanCrossCheck.getMinVL()
            ? SpartanCrossCheck.checkSpartan(player.uuid, checkName)
            : SpartanCrossCheck.CrossCheckResult.NOT_AVAILABLE;
        spartanSuffix = spartan.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED
            ? " [SAC+Spartan]" : "";

        player.punishmentManager.handleViolation(this);
        lastViolationTime = System.currentTimeMillis();
        violations = (maxVL > 0) ? Math.min(maxVL, violations + 1) : violations + 1;
        SpartanEventBridge.fireViolation(player, checkName, (int) violations, verbose);
        CheckPerformance.record(checkName, System.nanoTime() - start);
        return true;
    }

    public final boolean flagWithSetback() {
        return flagWithSetback("");
    }

    public final boolean flagWithSetback(String verbose) {
        if (flag(verbose)) {
            setbackIfAboveSetbackVL();
            return true;
        }
        return false;
    }

    public final boolean flagAndAlertWithSetback() {
        return flagAndAlertWithSetback("");
    }

    public final boolean flagAndAlertWithSetback(String verbose) {
        if (flagAndAlert(verbose)) {
            setbackIfAboveSetbackVL();
            return true;
        }
        return false;
    }

    public final void reward() {
        violations = Math.max(0, violations - decay);
    }

    @Override
    public final void reload(ConfigManager configuration) {
        ConfigManager effective = configuration;
        try {
            var registry = SacAPI.INSTANCE.getProfileRegistry();
            var profileCfg = SacAPI.INSTANCE.getProfileConfig();
            if (registry != null && profileCfg != null) {
                Profile p = registry.get(player.uuid);
                var section = profileCfg.snapshot().forProfile(p);
                effective = new ProfileAwareConfigView(configuration, section);
            }
        } catch (Throwable ignored) {} // null-safe during early bootstrap / tests

        decay = effective.getDoubleElse(configName + ".decay", decay);
        setbackVL = effective.getDoubleElse(configName + ".setbackvl", setbackVL);
        maxVL = effective.getDoubleElse(configName + ".maxvl",
            effective.getDoubleElse("punishment.max-vl", -1));
        punishWarnVL = effective.getIntElse(configName + ".punish-warn-vl", -1);
        punishKickVL = effective.getIntElse(configName + ".punish-kick-vl", -1);
        punishBanVL  = effective.getIntElse(configName + ".punish-ban-vl", -1);
        displayName = effective.getStringElse(configName + ".displayname", checkName);
        description = effective.getStringElse(configName + ".description", description);

        if (setbackVL == -1) setbackVL = Double.MAX_VALUE;
        onReload(effective);
    }

    @Override
    public void onReload(ConfigManager config) {

    }

    public boolean alert(String verbose) {
        try {
            String json = "{\"ts\":" + System.currentTimeMillis()
                + ",\"player\":\"" + player.getName() + "\""
                + ",\"uuid\":\"" + player.uuid + "\""
                + ",\"check\":\"" + checkName + "\""
                + ",\"vl\":" + ((int) violations)
                + ",\"ping\":" + player.getTransactionPing()
                + ",\"verbose\":\"" + (verbose + spartanSuffix).replace("\"", "'") + "\""
                + "}";
            SacAPI.INSTANCE.getProxyMessenger().sendAlert(player.uuid, json);
            SacAPI.INSTANCE.getAuditLogger().logAction(player.uuid, player.getName(),
                "VL_CHECK", checkName, verbose, true);
        } catch (Exception ignored) {}
        boolean handled = player.punishmentManager.handleAlert(player, verbose + spartanSuffix, this);
        // Legacy VL-threshold punisher (off by default) runs here, after the single
        // pipeline — never on the hot flag() path, so it cannot double-alert.
        if (AutoPunishment.isEnabled()) AutoPunishment.checkAndExecute(player, this);
        return handled;
    }

    public boolean setbackIfAboveSetbackVL() {
        if (shouldSetback()) {
            return player.getSetbackTeleportUtil().executeViolationSetback();
        }
        return false;
    }

    public boolean shouldSetback() {
        return !noSetbackPermission && violations > setbackVL;
    }

    public String formatOffset(double offset) {
        return offset > 0.001 ? String.format("%.5f", offset) : String.format("%.2E", offset);
    }

    public static boolean isTransaction(PacketTypeCommon packetType) {
        return packetType == PacketType.Play.Client.PONG ||
                packetType == PacketType.Play.Client.WINDOW_CONFIRMATION;
    }

    public static boolean isAsync(PacketTypeCommon packetType) {
        return packetType == PacketType.Play.Client.KEEP_ALIVE
                || packetType == PacketType.Play.Client.CHUNK_BATCH_ACK
                || packetType == PacketType.Play.Client.RESOURCE_PACK_STATUS;
    }

    public boolean isUpdate(PacketTypeCommon packetType) {
        return isFlying(packetType)
                || packetType == PacketType.Play.Client.CLIENT_TICK_END
                || isTransaction(packetType);
    }

    public boolean isTickPacket(PacketTypeCommon packetType) {
        if (isTickPacketIncludingNonMovement(packetType)) {
            if (isFlying(packetType)) {
                return !player.packetStateData.lastPacketWasTeleport && !player.packetStateData.lastPacketWasOnePointSeventeenDuplicate;
            }
            return true;
        }
        return false;
    }

    public boolean isTickPacketIncludingNonMovement(PacketTypeCommon packetType) {
        // On 1.21.2+ fall back to the TICK_END packet IF the player did not send a movement packet for their tick
        // TickTimer checks to see if player did not send a tick end packet before new flying packet is sent
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2)
                && !player.packetStateData.didSendMovementBeforeTickEnd) {
            if (packetType == PacketType.Play.Client.CLIENT_TICK_END) {
                return true;
            }
        }

        return isFlying(packetType);
    }

    // prevent causing exploits with packet cancelling (ie noslow)
    public boolean canCancel(DiggingAction action) {
        return action != DiggingAction.RELEASE_USE_ITEM
                // we check client version here because 1.8- doesn't predict dropping items, so we can cancel them. (see CompensatedInventory)
                && (action != DiggingAction.DROP_ITEM && action != DiggingAction.DROP_ITEM_STACK || player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8));
    }
}
