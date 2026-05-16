package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsAD", stableKey = "sac.badpackets.arm_animation_order", description = "Detects invalid arm animation packet order", setback = 5)
public class BadPacketsAD extends Check implements PacketCheck {

    private boolean sentAnimation = false;
    private boolean sentUse = false;
    private int flags = 0;

    public BadPacketsAD(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        var type = event.getPacketType();

        if (type == PacketType.Play.Client.ANIMATION) {
            sentAnimation = true;
        } else if (type == PacketType.Play.Client.USE_ITEM
                || type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            sentUse = true;
        } else if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                sentUse = true;
            }
        }

        if (WrapperPlayClientPlayerFlying.isFlying(type)) {
            if (sentUse && !sentAnimation) {
                flags++;
                if (flags > 3) {
                    flagAndAlert("use_without_swing flags=" + flags);
                }
            } else {
                flags = Math.max(0, flags - 1);
                reward();
            }
            sentAnimation = false;
            sentUse = false;
        }
    }
}
