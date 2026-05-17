package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "CrossAutoClicker", configName = "crossautoclicker", decay = 0.01, setback = 15, stableKey = "cross.autoclicker")
public class CrossAutoClicker extends Check implements PacketCheck {

    private int clickCount;
    private long lastReset;
    private int buffer;
    private static final int CLICK_THRESHOLD = 22;
    private static final double NETTY_VARIANCE_THRESHOLD = 15.0;

    public CrossAutoClicker(SacPlayer player) {
        super(player);
        lastReset = System.currentTimeMillis();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        long now = System.currentTimeMillis();
        if (now - lastReset > 1000) {
            clickCount = 0;
            lastReset = now;
        }

        boolean isAttack = false;
        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            isAttack = true;
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            isAttack = interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK;
        }

        if (!isAttack) return;

        clickCount++;
        if (clickCount < CLICK_THRESHOLD) return;

        boolean nettyConfirms = player.crossValidationData.nettyIntervalVariance < NETTY_VARIANCE_THRESHOLD
            && player.crossValidationData.nettyPacketRatePerSec > 15.0;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "AutoClicker");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 4) {
            flagAndAlert(String.format("cps=%d nettyVar=%.1f spartan=%s",
                clickCount, player.crossValidationData.nettyIntervalVariance, spartanResult.type()));
        }
    }
}
