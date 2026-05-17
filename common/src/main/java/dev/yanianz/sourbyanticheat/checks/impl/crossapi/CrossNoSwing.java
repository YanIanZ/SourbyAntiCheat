package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "CrossNoSwing", configName = "crossnoswing", decay = 0.02, setback = 5, stableKey = "cross.noswing")
public class CrossNoSwing extends Check implements PacketCheck {

    private int buffer;
    private boolean swingSent = false;

    // skipForBedrock defaults to true in Check — Bedrock/Geyser players legitimately suppress
    // swing animations, so they are exempted via the base class GeyserUtil check automatically.

    public CrossNoSwing(SacPlayer player) {
        super(player);
        // skipForBedrock = true (inherited default) — explicitly noted for clarity
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.compensatedEntities.self.isDead) return;

        if (player.inVehicle() || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            swingSent = true;
            return;
        }

        if (com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            swingSent = false;
            return;
        }

        boolean isAttack = false;
        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            isAttack = true;
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            isAttack = interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK;
        }

        if (!isAttack) return;

        if (!swingSent) {
            boolean nettyConfirms = player.crossValidationData.nettyIntervalVariance < 15.0;
            SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "NoSwing");
            boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;
            buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
            if (buffer > 3) {
                flagAndAlert(String.format("nettyVar=%.1f spartan=%s",
                    player.crossValidationData.nettyIntervalVariance, spartanResult.type()));
                return;
            }
        } else {
            buffer = Math.max(0, buffer - 1);
            // Fixed sparse-reward bug: reward() every clean tick, ungated by buffer < 2
            reward();
        }
    }
}
