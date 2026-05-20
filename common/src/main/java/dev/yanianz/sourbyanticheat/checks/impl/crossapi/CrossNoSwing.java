package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
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
    private int swingTicks;

    private double nettyVarianceThreshold = 12.0;
    private int swingWindow = 6;
    private static final int BUFFER_CAP = 5;

    public CrossNoSwing(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        nettyVarianceThreshold = config.getDoubleElse(base + "netty-variance-threshold", 12.0);
        swingWindow            = config.getIntElse(base + "swing-window", 6);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.compensatedEntities.self.isDead) return;
        if (player.inVehicle() || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            swingTicks = swingWindow;
            return;
        }
        if (com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            if (swingTicks > 0) swingTicks--;
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

        if (swingTicks <= 0) {
            boolean nettyConfirms = player.crossValidationData.nettyIntervalVariance < nettyVarianceThreshold;
            SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "NoSwing");
            boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

            if (spartanConfirms && nettyConfirms) {
                buffer = Math.min(BUFFER_CAP, buffer + 2);
            } else if (spartanConfirms || nettyConfirms) {
                buffer = Math.min(BUFFER_CAP, buffer + 1);
            } else {
                buffer = Math.max(0, buffer - 1);
                reward();
                return;
            }

            if (buffer >= 4) {
                flagAndAlert(String.format("nettyVar=%.1f vl=%d spartan=%s",
                    player.crossValidationData.nettyIntervalVariance, (int) violations,
                    spartanResult.type()));
                buffer = 0;
            }
        } else {
            buffer = Math.max(0, buffer - 1);
            reward();
        }
    }
}
