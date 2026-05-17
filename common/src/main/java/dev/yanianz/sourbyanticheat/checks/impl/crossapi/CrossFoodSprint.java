package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

// CrossNoSlowdown merged here (2026-05-18): both checks detected sprint-speed while using item;
// CrossNoSlowdown added a USE_TIMEOUT_MS guard which is folded in as the isUsingItem timeout branch.
// Config name "crossnoslowdown" dropped — use "crossfoodsprint" for both.
@CheckData(name = "CrossFoodSprint", configName = "crossfoodsprint", decay = 0.02, setback = 5, stableKey = "cross.foodsprint")
public class CrossFoodSprint extends Check implements PostPredictionCheck {

    private double buffer;
    private boolean isUsingItem = false;
    private long lastUseStart = 0;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private double sprintSpeed         = 0.28;
    private double nettyRateThreshold  = 15.0;
    private long   useTimeoutMs        = 5000L;

    public CrossFoodSprint(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        sprintSpeed        = config.getDoubleElse(base + "sprint-speed",          0.28);
        nettyRateThreshold = config.getDoubleElse(base + "netty-rate-threshold",  15.0);
        useTimeoutMs       = config.getIntElse(base + "use-timeout-ms",           5000);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            isUsingItem = true;
            lastUseStart = System.currentTimeMillis();
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            var dig = new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging(event);
            if (dig.getAction() == com.github.retrooper.packetevents.protocol.player.DiggingAction.RELEASE_USE_ITEM) {
                isUsingItem = false;
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.compensatedEntities.self.isDead) return;
        if (player.packetStateData.lastPacketWasTeleport) return;
        if (player.inVehicle() || player.canFly || player.isGliding) return;

        // Speed potion can legitimately exceed sprint-speed thresholds — exempt
        if (player.compensatedEntities.self.hasPotionEffect(PotionTypes.SPEED)) {
            buffer = Math.max(0, buffer - 0.02);
            reward();
            return;
        }

        // Expire stale USE_ITEM tracking (CrossNoSlowdown timeout guard)
        if (isUsingItem && System.currentTimeMillis() - lastUseStart > useTimeoutMs) {
            isUsingItem = false;
        }

        if (!isUsingItem || !player.isSprinting) {
            buffer = Math.max(0, buffer - 0.02);
            reward();
            return;
        }

        double deltaX = player.x - player.lastX;
        double deltaZ = player.z - player.lastZ;
        double speed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        boolean sprintingWhileUsing = speed > sprintSpeed;

        if (!sprintingWhileUsing) {
            buffer = Math.max(0, buffer - 0.02);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > nettyRateThreshold;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "NoSlowdown");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 1.5 : 0.5;
        if (buffer > 3.0) {
            flagAndAlert(String.format("speed=%.2f netty=%.1f/s spartan=%s",
                speed, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
            return;
        }
    }
}
