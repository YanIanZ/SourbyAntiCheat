package dev.yanianz.sourbyanticheat.profile.leniency.handlers;

import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyEventBus;
import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyId;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

public final class MLGWaterLandHandler implements Listener {
    private final LeniencyEventBus bus;
    public MLGWaterLandHandler(LeniencyEventBus bus) { this.bus = bus; }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (e.getBucket() != Material.WATER_BUCKET) return;
        var p = e.getPlayer();
        if (p.isOnGround()) return;
        if (p.getFallDistance() < 3f) return;
        bus.fire(LeniencyId.MLG_WATER_LAND, p.getUniqueId(), 0);
    }
}
