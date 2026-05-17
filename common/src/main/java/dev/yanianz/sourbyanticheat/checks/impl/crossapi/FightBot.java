package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;
import dev.yanianz.sourbyanticheat.utils.data.packetentity.PacketEntity;

@CheckData(name = "FightBot", configName = "fightbot", decay = 0.01, setback = 15, stableKey = "cross.fightbot")
public class FightBot extends Check implements PostPredictionCheck {

    private int buffer;
    private int perfectAimStreak;
    private int attackedEntity = -1;

    // Config-wired thresholds.
    // combinedAimDeltaThreshold gates the combined yaw+pitch angular error magnitude
    // sqrt(yawDiff^2 + pitchDiff^2) — see onPredictionComplete. The audit flagged the
    // prior yaw-only metric as a false-positive source (legit players with perfect yaw
    // but imperfect pitch were treated as bots). Requiring both axes to be near-perfect
    // is the intended FP-reduction fix, so the metric is deliberately stricter than the
    // old yaw-only check; the default keeps the prior 2.0 numeric value.
    private double combinedAimDeltaThreshold = 2.0;
    private double peRotationDeltaYawThreshold = 5.0;
    private int perfectAimStreakThreshold = 5;

    public FightBot(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        combinedAimDeltaThreshold   = config.getDoubleElse(base + "combined-aim-delta-threshold",    2.0);
        peRotationDeltaYawThreshold = config.getDoubleElse(base + "pe-rotation-delta-yaw-threshold", 5.0);
        perfectAimStreakThreshold   = config.getIntElse(base + "perfect-aim-streak-threshold",       5);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;
        WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
        if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            attackedEntity = interact.getEntityId();
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        if (attackedEntity < 0) {
            perfectAimStreak = Math.max(0, perfectAimStreak - 1);
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR
                || player.inVehicle() || player.compensatedEntities.self.isDead) {
            attackedEntity = -1;
            return;
        }

        PacketEntity entity = player.compensatedEntities.entityMap.get(attackedEntity);
        attackedEntity = -1;
        if (entity == null || entity.isDead) return;

        double ex = entity.trackedServerPosition.getPos().getX();
        double ey = entity.trackedServerPosition.getPos().getY();
        double ez = entity.trackedServerPosition.getPos().getZ();

        double dx = ex - player.x;
        double dz = ez - player.z;
        // Aim target ~1.0 above the entity's feet (upper body of a player-sized entity).
        double dy = (ey + 1.0) - (player.y + player.getEyeHeight());
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        double yawToEntity = Math.toDegrees(Math.atan2(dx, dz));
        double yawDelta = Math.abs(player.yaw - yawToEntity);
        double yawDiff = yawDelta > 180 ? 360 - yawDelta : yawDelta;

        double pitchToEntity = -Math.toDegrees(Math.atan2(dy, horizontalDist));
        double pitchDiff = Math.abs(player.pitch - pitchToEntity);

        // Combine yaw and pitch error into a single angular delta to the entity.
        // Both axes must be near-perfect for this to fall under the threshold —
        // perfect yaw alone no longer flags (FP-reduction, see field comment).
        double combinedAimDelta = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

        if (combinedAimDelta < combinedAimDeltaThreshold
                && Math.abs(player.crossValidationData.peRotationDeltaYaw) > peRotationDeltaYawThreshold) {
            perfectAimStreak++;
        } else {
            perfectAimStreak = Math.max(0, perfectAimStreak - 1);
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        if (perfectAimStreak < perfectAimStreakThreshold) return;

        boolean nettyConfirms = player.crossValidationData.nettyIntervalVariance < 15.0;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "KillAura");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 4) {
            flagAndAlert(String.format("aimErr=%.1f streak=%d nettyVar=%.1f spartan=%s",
                combinedAimDelta, perfectAimStreak, player.crossValidationData.nettyIntervalVariance, spartanResult.type()));
        }
    }
}
