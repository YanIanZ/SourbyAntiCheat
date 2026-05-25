package dev.yanianz.sourbyanticheat.profile.leniency.handlers;

import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyEventBus;
import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyId;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

class MLGWaterLandHandlerTest {
    @Test void firesWhenAirborneAndFalling() {
        var bus = mock(LeniencyEventBus.class);
        var player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        when(player.isOnGround()).thenReturn(false);
        when(player.getFallDistance()).thenReturn(8f);
        when(player.getVelocity()).thenReturn(new Vector(0, -1.5, 0));
        var event = mock(PlayerBucketEmptyEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getBucket()).thenReturn(Material.WATER_BUCKET);

        new MLGWaterLandHandler(bus).onBucketEmpty(event);
        verify(bus).fire(LeniencyId.MLG_WATER_LAND, id, 0);
    }
    @Test void ignoresGroundedPlayer() {
        var bus = mock(LeniencyEventBus.class);
        var player = mock(Player.class);
        when(player.isOnGround()).thenReturn(true);
        var event = mock(PlayerBucketEmptyEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getBucket()).thenReturn(Material.WATER_BUCKET);
        new MLGWaterLandHandler(bus).onBucketEmpty(event);
        verifyNoInteractions(bus);
    }
    @Test void ignoresNonWaterBucket() {
        var bus = mock(LeniencyEventBus.class);
        var player = mock(Player.class);
        when(player.isOnGround()).thenReturn(false);
        var event = mock(PlayerBucketEmptyEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getBucket()).thenReturn(Material.LAVA_BUCKET);
        new MLGWaterLandHandler(bus).onBucketEmpty(event);
        verifyNoInteractions(bus);
    }
}
