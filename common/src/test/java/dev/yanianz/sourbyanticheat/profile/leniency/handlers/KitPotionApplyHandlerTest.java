package dev.yanianz.sourbyanticheat.profile.leniency.handlers;

import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyEventBus;
import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyId;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.UUID;

import static org.mockito.Mockito.*;

class KitPotionApplyHandlerTest {
    @BeforeAll static void setup() { MockBukkit.mock(); }
    @AfterAll static void teardown() { MockBukkit.unmock(); }

    @Test void firesOnSpeedPotionWithAmplifier() {
        var bus = mock(LeniencyEventBus.class);
        var player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);

        var effect = new PotionEffect(PotionEffectType.SPEED, 200, 1);
        var event = mock(EntityPotionEffectEvent.class);
        when(event.getEntity()).thenReturn(player);
        when(event.getNewEffect()).thenReturn(effect);
        when(event.getAction()).thenReturn(EntityPotionEffectEvent.Action.ADDED);

        new KitPotionApplyHandler(bus).onPotion(event);
        verify(bus).fire(LeniencyId.KIT_POTION_APPLY, id, 1);
    }
    @Test void ignoresNonPlayerEntity() {
        var bus = mock(LeniencyEventBus.class);
        var event = mock(EntityPotionEffectEvent.class);
        when(event.getEntity()).thenReturn(mock(org.bukkit.entity.Zombie.class));
        new KitPotionApplyHandler(bus).onPotion(event);
        verifyNoInteractions(bus);
    }
    @Test void ignoresIrrelevantPotionType() {
        var bus = mock(LeniencyEventBus.class);
        var player = mock(Player.class);
        var effect = new PotionEffect(PotionEffectType.POISON, 200, 0);
        var event = mock(EntityPotionEffectEvent.class);
        when(event.getEntity()).thenReturn(player);
        when(event.getNewEffect()).thenReturn(effect);
        when(event.getAction()).thenReturn(EntityPotionEffectEvent.Action.ADDED);
        new KitPotionApplyHandler(bus).onPotion(event);
        verifyNoInteractions(bus);
    }
    @Test void ignoresRemovedAction() {
        var bus = mock(LeniencyEventBus.class);
        var player = mock(Player.class);
        var effect = new PotionEffect(PotionEffectType.SPEED, 200, 0);
        var event = mock(EntityPotionEffectEvent.class);
        when(event.getEntity()).thenReturn(player);
        when(event.getNewEffect()).thenReturn(effect);
        when(event.getAction()).thenReturn(EntityPotionEffectEvent.Action.REMOVED);
        new KitPotionApplyHandler(bus).onPotion(event);
        verifyNoInteractions(bus);
    }
}
