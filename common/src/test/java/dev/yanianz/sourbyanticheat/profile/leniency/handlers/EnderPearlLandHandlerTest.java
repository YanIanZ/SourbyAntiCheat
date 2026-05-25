package dev.yanianz.sourbyanticheat.profile.leniency.handlers;

import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyEventBus;
import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyId;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

class EnderPearlLandHandlerTest {
    @Test void firesOnPlayerPearlHit() {
        var bus = mock(LeniencyEventBus.class);
        var player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        var pearl = mock(EnderPearl.class);
        when(pearl.getShooter()).thenReturn(player);
        var event = new ProjectileHitEvent(pearl);

        new EnderPearlLandHandler(bus).onHit(event);

        verify(bus).fire(LeniencyId.ENDER_PEARL_LAND, id, 0);
    }
    @Test void ignoresNonPearl() {
        var bus = mock(LeniencyEventBus.class);
        var arrow = mock(org.bukkit.entity.Arrow.class);
        var event = new ProjectileHitEvent(arrow);
        new EnderPearlLandHandler(bus).onHit(event);
        verifyNoInteractions(bus);
    }
    @Test void ignoresNonPlayerShooter() {
        var bus = mock(LeniencyEventBus.class);
        var pearl = mock(EnderPearl.class);
        when(pearl.getShooter()).thenReturn(mock(org.bukkit.projectiles.ProjectileSource.class));
        var event = new ProjectileHitEvent(pearl);
        new EnderPearlLandHandler(bus).onHit(event);
        verifyNoInteractions(bus);
    }
}
