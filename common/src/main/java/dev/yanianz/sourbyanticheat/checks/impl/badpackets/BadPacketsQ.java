package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction.Action;

@CheckData(name = "BadPacketsQ", stableKey = "sac.badpackets.invalid_horse_jump")
public class BadPacketsQ extends Check implements PacketCheck {

    // Config-wired threshold (default equals prior hardcoded value)
    private int maxJumpBoost = 100;

    public BadPacketsQ(final SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        maxJumpBoost = config.getIntElse(getConfigName() + ".max-jump-boost", 100);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction wrapper = new WrapperPlayClientEntityAction(event);

            // you are able to send negative jump boost, how and why!?
            // Bad jump boost: out-of-range magnitude, OR a non-zero boost paired with an
            // action other than START_JUMPING_WITH_HORSE. The grouping is explicit so the
            // entity-ID mismatch only flags when it actually accompanies a jump action.
            boolean badBoost = Math.abs(wrapper.getJumpBoost()) > maxJumpBoost
                    || (wrapper.getAction() != Action.START_JUMPING_WITH_HORSE && wrapper.getJumpBoost() != 0);
            boolean spoofedEntity = wrapper.getJumpBoost() != 0 && wrapper.getEntityId() != player.entityID;

            if (badBoost || spoofedEntity) {
                if (flagAndAlert("boost=" + wrapper.getJumpBoost() + ", action=" + wrapper.getAction() + ", entity=" + wrapper.getEntityId()) && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            } else {
                reward();
            }
        }
    }
}
