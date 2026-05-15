package dev.yanianz.sourbyanticheat.checks.impl.misc;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;

@CheckData(name = "PayloadCheck", stableKey = "sac.packet.payload", description = "Detects malicious custom payload packets", setback = 10)
public class PayloadCheck extends Check implements PacketCheck {

    private static final int MAX_PAYLOAD_SIZE = 32767;
    private int flaggedCount = 0;

    public PayloadCheck(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLUGIN_MESSAGE) return;

        WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
        String channel = packet.getChannelName();
        byte[] data = packet.getData();

        if (data.length > MAX_PAYLOAD_SIZE) {
            flagAndAlert("oversized=" + data.length + " channel=" + channel);
            return;
        }

        if (channel.startsWith("sac:") || channel.startsWith("grim:")) {
            return;
        }

        if (data.length > 8192 && channel.startsWith("MC|")) {
            flaggedCount++;
            if (flaggedCount > 3) {
                flagAndAlert("suspicious channel=" + channel + " size=" + data.length);
            }
        } else if (flaggedCount > 0) {
            flaggedCount = Math.max(0, flaggedCount - 1);
            reward();
        }
    }
}
