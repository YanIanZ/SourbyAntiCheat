package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PostPredictionCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.PredictionComplete;

@CheckData(name = "CrossFastEat", configName = "crossfasteat", decay = 0.02, setback = 5, stableKey = "cross.fasteat")
public class CrossFastEat extends Check implements PostPredictionCheck {

    private double buffer;
    private long useStartTime = 0;
    private boolean isUsing = false;
    private boolean isFood = false; // true = food item, false = non-food consumable (potion etc)

    // Vanilla food time: 32 ticks = ~1600ms. Config default = 1400ms (latency tolerance).
    private long minEatTime   = 1400;
    // Potions also take 32 ticks, same tolerance by default.
    private long minDrinkTime = 1400;

    private static final double NETTY_RATE_THRESHOLD = 120.0;

    public CrossFastEat(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.minEatTime   = config.getIntElse(base + "min-eat-time",   1400);
        this.minDrinkTime = config.getIntElse(base + "min-drink-time", 1400);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.disableGrim) return;

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            var hand = player.inventory.getHeldItem();
            ItemType type = hand.getType();
            // Track whether the used item is food (EDIBLE) or a non-food consumable (potion etc)
            if (type != null && type.hasAttribute(ItemTypes.ItemAttribute.EDIBLE)) {
                useStartTime = System.currentTimeMillis();
                isUsing = true;
                isFood = true;
            } else if (type == ItemTypes.POTION || type == ItemTypes.MILK_BUCKET
                    || type == ItemTypes.HONEY_BOTTLE) {
                useStartTime = System.currentTimeMillis();
                isUsing = true;
                isFood = false;
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            var dig = new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging(event);
            if (dig.getAction() == com.github.retrooper.packetevents.protocol.player.DiggingAction.RELEASE_USE_ITEM) {
                isUsing = false;
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete complete) {
        if (player.disableGrim) return;
        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) return;

        if (!isUsing || useStartTime == 0) {
            buffer = Math.max(0, buffer - 0.02);
            reward();
            return;
        }

        long elapsed = System.currentTimeMillis() - useStartTime;
        if (elapsed > 5000 || elapsed < 100) {
            isUsing = false;
            reward();
            return;
        }

        long minTime = isFood ? minEatTime : minDrinkTime;
        boolean fastConsume = elapsed < minTime;

        if (!fastConsume) {
            buffer = Math.max(0, buffer - 0.02);
            reward();
            return;
        }

        boolean nettyConfirms = player.crossValidationData.nettyPacketRatePerSec > NETTY_RATE_THRESHOLD;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "FastEat");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 1.5 : 0.5;
        if (buffer > 3.0) {
            flagAndAlert(String.format("eat=%dms min=%dms food=%b netty=%.1f/s spartan=%s",
                elapsed, minTime, isFood,
                player.crossValidationData.nettyPacketRatePerSec, spartanResult.type()));
        }
        isUsing = false;
    }
}
