// This file is part of SourbyAntiCheat
// Copyright (C) 2026 YanIanZ
// Licensed under GPLv3 - see LICENSE file for details

package dev.yanianz.sourbyanticheat.checks.impl.combat;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.PacketCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

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
    private int flags = 0;

    // Vanilla eat time = 32 ticks (~1600ms). Use 1400ms tolerance for latency.
    private static final long MIN_EAT_TIME = 1400;
    // Potion drink time is also 32 ticks normally
    private static final long MIN_DRINK_TIME = 1400;

    public FastEat(SacPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            var mainHand = player.inventory.getHeldItem();
            var offHand = player.inventory.getOffHand();

            boolean holdingConsumable = isConsumable(mainHand.getType()) || isConsumable(offHand.getType());
            if (holdingConsumable) {
                useStartTime = System.currentTimeMillis();
                isUsing = true;
            }
        }

        // RELEASE_USE_ITEM or item switches means the use is done
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            var digging = new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging(event);
            if (digging.getAction() == com.github.retrooper.packetevents.protocol.player.DiggingAction.RELEASE_USE_ITEM) {
                if (isUsing) {
                    long elapsed = System.currentTimeMillis() - useStartTime;
                    if (elapsed < MIN_EAT_TIME && elapsed > 0) {
                        flags++;
                        if (flags > 3) {
                            flagAndAlert("eat=" + elapsed + "ms flags=" + flags);
                        }
                    } else {
                        flags = Math.max(0, flags - 1);
                        if (flags < 1) reward();
                    }
                    isUsing = false;
                }
            }
        }
    }

    private static boolean isConsumable(com.github.retrooper.packetevents.protocol.item.type.ItemType type) {
        if (type == null) return false;
        // Check for common consumable items
        return type == ItemTypes.POTION || type == ItemTypes.SPLASH_POTION
                || type == ItemTypes.GOLDEN_APPLE || type == ItemTypes.ENCHANTED_GOLDEN_APPLE
                || type == ItemTypes.MILK_BUCKET || type == ItemTypes.HONEY_BOTTLE
                || type == ItemTypes.MUSHROOM_STEW || type == ItemTypes.RABBIT_STEW
                || type == ItemTypes.BEETROOT_SOUP || type == ItemTypes.SUSPICIOUS_STEW
                || type.getName().getKey().contains("cooked_")
                || type.getName().getKey().contains("_apple")
                || type.getName().getKey().contains("bread")
                || type.getName().getKey().contains("_beef")
                || type.getName().getKey().contains("_pork")
                || type.getName().getKey().contains("_chicken")
                || type.getName().getKey().contains("_mutton")
                || type.getName().getKey().contains("_salmon")
                || type.getName().getKey().contains("_cod");
    }
}
