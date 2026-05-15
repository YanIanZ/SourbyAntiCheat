package dev.yanianz.sourbyanticheat.utils.item;

import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.latency.CompensatedWorld;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;

public class AlwaysUseItem extends ItemBehaviour {

    public static final AlwaysUseItem INSTANCE = new AlwaysUseItem();

    @Override
    public boolean canUse(ItemStack item, CompensatedWorld world, SacPlayer player, InteractionHand hand) {
        return true;
    }

}
