// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

/**
 * Improved FastEat detection using PacketEvents item tracking.
 * <p>
 * Vanilla eating takes 32 ticks (1600ms). Checks that the player is actually
 * holding a food/potion item (not just any item) and verifies the consume
 * duration is plausible.
 */
@CheckData(name = "FastEat", stableKey = "sac.combat.fasteat", description = "Detects fast eating/healing via packet timing", setback = 5, decay = 0.02)
public class FastEat extends Check implements PacketCheck {

    private long useStartTime = 0;
    private boolean isUsing = false;
    // true = food item (EDIBLE), false = non-food drinkable consumable (potion etc).
    private boolean isFood = false;
    private int flags = 0;

    // Config-wired thresholds (defaults equal prior hardcoded values)
    // Vanilla eat time = 32 ticks (~1600ms). Use 1400ms tolerance for latency.
    private long minEatTime = 1400;
    // Potion drink time is also 32 ticks normally.
    private long minDrinkTime = 1400;
    private int flagThreshold = 3;
    private int minFlag = 1;

    public FastEat(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        String base = getConfigName() + ".";
        this.minEatTime = config.getIntElse(base + "min-eat-time", 1400);
        this.minDrinkTime = config.getIntElse(base + "min-drink-time", 1400);
        this.flagThreshold = config.getIntElse(base + "flag-threshold", 3);
        this.minFlag = config.getIntElse(base + "min-flag", 1);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // Reset use state on death and teleport — an in-progress use is no longer valid.
        if (player.compensatedEntities.self.isDead
                || player.packetStateData.lastPacketWasTeleport) {
            isUsing = false;
        }

        // Switching the held hotbar slot cancels an in-progress consume.
        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            isUsing = false;
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            var mainHand = player.inventory.getHeldItem();
            var offHand = player.inventory.getOffHand();

            ItemType mainType = mainHand.getType();
            ItemType offType = offHand.getType();
            if (isFood(mainType) || isFood(offType)) {
                useStartTime = System.currentTimeMillis();
                isUsing = true;
                isFood = true;
            } else if (isDrinkable(mainType) || isDrinkable(offType)) {
                useStartTime = System.currentTimeMillis();
                isUsing = true;
                isFood = false;
            }
        }

        // RELEASE_USE_ITEM means the use is done.
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            var digging = new WrapperPlayClientPlayerDigging(event);
            if (digging.getAction() == DiggingAction.RELEASE_USE_ITEM && isUsing) {
                // Creative players consume instantly — never flag them.
                if (player.gamemode == GameMode.CREATIVE) {
                    isUsing = false;
                    return;
                }

                long elapsed = System.currentTimeMillis() - useStartTime;
                long minTime = isFood ? minEatTime : minDrinkTime;
                if (elapsed < minTime && elapsed > 0) {
                    flags++;
                    if (flags > flagThreshold) {
                        flagAndAlert("consume=" + elapsed + "ms min=" + minTime + "ms food=" + isFood
                                + " flags=" + flags);
                    }
                } else {
                    flags = Math.max(0, flags - 1);
                    if (flags < minFlag) reward();
                }
                isUsing = false;
            }
        }
    }

    /** Food items — identified by the EDIBLE attribute rather than fragile name matching. */
    private static boolean isFood(ItemType type) {
        return type != null && type.hasAttribute(ItemTypes.ItemAttribute.EDIBLE);
    }

    /** Non-food drinkable consumables — these are not EDIBLE but still take time to use. */
    private static boolean isDrinkable(ItemType type) {
        return type == ItemTypes.POTION || type == ItemTypes.MILK_BUCKET
                || type == ItemTypes.HONEY_BOTTLE;
    }
}
