package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsAD", stableKey = "sac.badpackets.arm_animation_order", description = "Detects invalid arm animation packet order", setback = 5)
public class BadPacketsAD extends Check implements PacketCheck {

    private boolean sentAnimation = false;
    private boolean sentUse = false;
    private int flags = 0;

    private int flagThreshold = 3;

    public BadPacketsAD(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        flagThreshold = config.getIntElse(getConfigName() + ".flag-threshold", 3);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        var type = event.getPacketType();

        if (type == PacketType.Play.Client.ANIMATION) {
            sentAnimation = true;
        } else if (type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            // Only an actual block placement animates the arm; PLAYER_BLOCK_PLACEMENT
            // with face OTHER is item use (eat/drink/bow/trident/shield) which is
            // animation-free and must not require a swing.
            if (new WrapperPlayClientPlayerBlockPlacement(event).getFace() != BlockFace.OTHER) {
                sentUse = true;
            }
        }
        // USE_ITEM and non-attack INTERACT_ENTITY are animation-free interactions
        // (eating, drinking, drawing a bow, trading, naming) — intentionally NOT
        // tracked: requiring a swing for them produced false positives.

        if (WrapperPlayClientPlayerFlying.isFlying(type)) {
            if (sentUse && !sentAnimation) {
                flags++;
                if (flags > flagThreshold) {
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
