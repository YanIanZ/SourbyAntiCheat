package dev.yanianz.sourbyanticheat.profile.leniency.handlers;

import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyEventBus;
import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyId;
import org.bukkit.ExplosionResult;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

class FireballBoostHandlerTest {
    @Test void firesForPlayerWithin4Blocks() {
        var bus = mock(LeniencyEventBus.class);
        var world = mock(World.class);
        var origin = new Location(world, 0, 64, 0);
        var nearby = mock(Player.class);
        UUID nearbyId = UUID.randomUUID();
        when(nearby.getUniqueId()).thenReturn(nearbyId);
        when(nearby.getLocation()).thenReturn(new Location(world, 2, 64, 0));
        when(world.getNearbyEntities(origin, 4, 4, 4)).thenReturn(List.of(nearby));

        var fireball = mock(Fireball.class);
        when(fireball.getLocation()).thenReturn(origin);
        var event = new EntityExplodeEvent(fireball, origin, List.<Block>of(), 0f, ExplosionResult.DESTROY);

        new FireballBoostHandler(bus).onExplode(event);
        verify(bus).fire(LeniencyId.FIREBALL_BOOST, nearbyId, 0);
    }
    @Test void ignoresNonFireball() {
        var bus = mock(LeniencyEventBus.class);
        var tnt = mock(org.bukkit.entity.TNTPrimed.class);
        var loc = new Location(mock(World.class), 0, 0, 0);
        var event = new EntityExplodeEvent(tnt, loc, List.<Block>of(), 0f, ExplosionResult.DESTROY);
        new FireballBoostHandler(bus).onExplode(event);
        verifyNoInteractions(bus);
    }
}
