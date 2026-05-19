package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;

@CheckData(name = "FastHeal", configName = "fastheal", decay = 0.02, setback = 5, stableKey = "cross.fastheal")
public class FastHeal extends Check implements PacketCheck {

    private int healCount;
    private long lastReset;
    private int buffer;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    private int healThreshold          = 5;
    private double nettyRateThreshold  = 120.0;

    public FastHeal(SacPlayer player) {
        super(player);
        lastReset = System.currentTimeMillis();
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        healThreshold      = config.getIntElse(base + "heal-threshold",          5);
        nettyRateThreshold = config.getDoubleElse(base + "netty-rate-threshold", 120.0);
    }

    /** True only for items whose use restores health/hunger — food, potions, milk, honey. */
    private static boolean isHealingItem(ItemType type) {
        if (type == null) return false;
        return type.hasAttribute(ItemTypes.ItemAttribute.EDIBLE)
            || type == ItemTypes.POTION
            || type == ItemTypes.MILK_BUCKET
            || type == ItemTypes.HONEY_BOTTLE;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        long now = System.currentTimeMillis();
        if (now - lastReset > 1000) { healCount = 0; lastReset = now; }

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM
                && isHealingItem(player.inventory.getHeldItem().getType())) {
            healCount++;
        }

        if (healCount < healThreshold) { reward(); return; }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > nettyRateThreshold;
        SpartanCrossCheck.CrossCheckResult spartanResult = SpartanCrossCheck.checkSpartan(player.uuid, "FastHeal");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 3) {
            flagAndAlert(String.format("healItems=%d/s netty=%.1f/s spartan=%s",
                healCount, player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
    }
}
