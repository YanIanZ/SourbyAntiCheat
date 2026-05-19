package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "CrossFastBow", configName = "crossfastbow", decay = 0.02, setback = 5, stableKey = "cross.fastbow")
public class CrossFastBow extends Check implements PacketCheck {

    private int buffer;
    private long drawStart = 0;
    private boolean isDrawing = false;

    private long minChargeTime = 100;
    private static final double NETTY_RATE_THRESHOLD = 120.0;

    public CrossFastBow(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        this.minChargeTime = config.getIntElse(getConfigName() + ".min-charge-time", 100);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;
        if (player.compensatedEntities.self.isDead) return;

        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT
                || event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            var hand = player.inventory.getHeldItem();
            if (hand.getType() == ItemTypes.BOW || hand.getType() == ItemTypes.CROSSBOW) {
                drawStart = System.currentTimeMillis();
                isDrawing = true;
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            var dig = new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging(event);
            if (dig.getAction() == com.github.retrooper.packetevents.protocol.player.DiggingAction.RELEASE_USE_ITEM && isDrawing) {
                long charge = System.currentTimeMillis() - drawStart;
                isDrawing = false;

                // Add player RTT as tolerance: a high-ping player's server-measured charge
                // appears shorter than the real client-side duration, so we grant the full
                // ping as benefit of the doubt. Flag only when charge + ping is still below
                // minChargeTime — meaning even with leniency the shot was impossibly fast.
                long ping = player.getTransactionPing();

                if (charge > 0 && charge + ping < minChargeTime) {
                    boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;
                    SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "FastBow");
                    boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;
                    buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
                    if (buffer > 3) {
                        flagAndAlert(String.format("charge=%dms tolerant=%dms ping=%dms netty=%.1f/s spartan=%s",
                            charge, charge + ping, ping,
                            player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
                    }
                } else {
                    buffer = Math.max(0, buffer - 1);
                    reward();
                }
            }
        }
    }
}
