package dev.yanianz.sourbyanticheat.profile.leniency.handlers;

import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyEventBus;
import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyId;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

class ElytraFireworkBoostHandlerTest {
    @Test void firesOnElytraBoost() {
        var bus = mock(LeniencyEventBus.class);
        var player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        var event = new PlayerElytraBoostEvent(player, mock(ItemStack.class), mock(Firework.class), EquipmentSlot.HAND);
        new ElytraFireworkBoostHandler(bus).onBoost(event);
        verify(bus).fire(LeniencyId.ELYTRA_FIREWORK_BOOST, id, 0);
    }
}
