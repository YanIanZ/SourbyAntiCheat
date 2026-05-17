package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "BackTrack", configName = "backtrack", decay = 0.02, setback = 10, stableKey = "cross.backtrack")
public class BackTrack extends Check implements PostPredictionCheck {

    private int buffer;
    private boolean attackedThisTick = false;
    private static final double NETTY_RATE_THRESHOLD = 15.0;

    public BackTrack(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            attackedThisTick = interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;

        if (!attackedThisTick) {
            buffer = Math.max(0, buffer - 1);
            reward();
            attackedThisTick = false;
            return;
        }
        attackedThisTick = false;

        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;
        if (player.compensatedEntities.self.isDead) return;

        if (player.packetStateData.lastClaimedPosition == null) return;

        double dx = player.x - player.packetStateData.lastClaimedPosition.getX();
        double dz = player.z - player.packetStateData.lastClaimedPosition.getZ();
        double mismatch = Math.sqrt(dx * dx + dz * dz);

        if (mismatch < 1.5) {
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "Reach");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 3) {
            flagAndAlert(String.format("mismatch=%.1f netty=%.1f/s spartan=%s",
                mismatch, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
