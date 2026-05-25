package dev.yanianz.sourbyanticheat.profile.leniency.handlers;

import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyEventBus;
import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyId;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

class RodPullHandlerTest {
    @Test void firesOnEntityCaughtAgainstPlayer() {
        var bus = mock(LeniencyEventBus.class);
        var puller = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(puller.getUniqueId()).thenReturn(id);
        var target = mock(Player.class);
        var hook = mock(FishHook.class);
        var event = new PlayerFishEvent(puller, target, hook, PlayerFishEvent.State.CAUGHT_ENTITY);
        new RodPullHandler(bus).onFish(event);
        verify(bus).fire(LeniencyId.ROD_PULL, id, 0);
    }
    @Test void ignoresNonEntityCatch() {
        var bus = mock(LeniencyEventBus.class);
        var puller = mock(Player.class);
        var hook = mock(FishHook.class);
        var event = new PlayerFishEvent(puller, null, hook, PlayerFishEvent.State.FISHING);
        new RodPullHandler(bus).onFish(event);
        verifyNoInteractions(bus);
    }
    @Test void ignoresWhenCaughtIsNotPlayer() {
        var bus = mock(LeniencyEventBus.class);
        var puller = mock(Player.class);
        var hook = mock(FishHook.class);
        var event = new PlayerFishEvent(puller, mock(org.bukkit.entity.Cow.class), hook,
                PlayerFishEvent.State.CAUGHT_ENTITY);
        new RodPullHandler(bus).onFish(event);
        verifyNoInteractions(bus);
    }
}
