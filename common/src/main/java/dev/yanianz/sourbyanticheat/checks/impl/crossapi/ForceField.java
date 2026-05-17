package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "ForceField", configName = "forcefield", decay = 0.02, setback = 10, stableKey = "cross.forcefield")
public class ForceField extends Check implements PacketCheck {

    private int attacksThisTick;
    private int lastEntity;
    private int buffer;

    public ForceField(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.compensatedEntities.self.isDead) return;

        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        if (com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            if (attacksThisTick > 1) {
                boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > 15.0;
                SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "KillAura");
                boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;
                buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
                if (buffer > 3) {
                    flagAndAlert(String.format("attacks=%d netty=%.1f/s spartan=%s",
                        attacksThisTick, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
                }
            } else {
                buffer = Math.max(0, buffer - 1);
                if (buffer < 2) reward();
            }
            attacksThisTick = 0;
            lastEntity = -1;
            return;
        }

        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;
        WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
        if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        int eid = interact.getEntityId();
        if (eid != lastEntity) {
            attacksThisTick++;
            lastEntity = eid;
        }
    }
}
