package dev.yanianz.sourbyanticheat.checks.impl.breaking;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.BlockBreakCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.GameMode;

@CheckData(name = "NoSwingBreak", stableKey = "sac.breaking.no_swing_break", description = "Did not swing while breaking block")
public class NoSwingBreak extends Check implements BlockBreakCheck {
    private boolean sentAnimation;
    private boolean sentBreak;

    public NoSwingBreak(SacPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action != DiggingAction.CANCELLED_DIGGING) {
            sentBreak = true;
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            sentAnimation = true;
        }

        if (isTickPacket(event.getPacketType())) {
            if (sentBreak && !sentAnimation && !isExempt()) {
                flagAndAlert();
            } else if (sentBreak) {
                reward();
            }

            sentAnimation = sentBreak = false;
        }
    }

    private boolean isExempt() {
        // Creative/spectator: digging packets and swing animations are not paired the same way.
        if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR) return true;
        // Riding an entity changes the client's animation handling.
        if (player.inVehicle()) return true;
        // 1.8 clients order the swing animation differently relative to the digging packet —
        // the animation can land in a different tick than the break, so don't flag it.
        return player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8);
    }
}
