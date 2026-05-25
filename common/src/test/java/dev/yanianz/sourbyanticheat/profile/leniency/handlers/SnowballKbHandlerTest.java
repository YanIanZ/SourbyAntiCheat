package dev.yanianz.sourbyanticheat.profile.leniency.handlers;

import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyEventBus;
import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyId;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

class SnowballKbHandlerTest {
    static EntityDamageByEntityEvent damage(Entity damager, Entity victim) {
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(damager);
        when(event.getEntity()).thenReturn(victim);
        return event;
    }

    @Test void firesOnSnowballDamageToPlayer() {
        var bus = mock(LeniencyEventBus.class);
        var victim = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(victim.getUniqueId()).thenReturn(id);
        var snowball = mock(Snowball.class);
        new SnowballKbHandler(bus).onDamage(damage(snowball, victim));
        verify(bus).fire(LeniencyId.SNOWBALL_KB, id, 0);
    }
    @Test void ignoresArrow() {
        var bus = mock(LeniencyEventBus.class);
        var victim = mock(Player.class);
        var arrow = mock(Arrow.class);
        new SnowballKbHandler(bus).onDamage(damage(arrow, victim));
        verifyNoInteractions(bus);
    }
    @Test void ignoresNonPlayerVictim() {
        var bus = mock(LeniencyEventBus.class);
        var victim = mock(Zombie.class);
        var snowball = mock(Snowball.class);
        new SnowballKbHandler(bus).onDamage(damage(snowball, victim));
        verifyNoInteractions(bus);
    }
}
