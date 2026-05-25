package dev.yanianz.sourbyanticheat.profile.leniency.handlers;

import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyEventBus;
import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyId;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

public final class FireballBoostHandler implements Listener {
    private final LeniencyEventBus bus;
    public FireballBoostHandler(LeniencyEventBus bus) { this.bus = bus; }

    @EventHandler
    public void onExplode(EntityExplodeEvent e) {
        if (!(e.getEntity() instanceof Fireball)) return;
        var origin = e.getLocation();
        for (var ent : origin.getWorld().getNearbyEntities(origin, 4, 4, 4)) {
            if (ent instanceof Player p) bus.fire(LeniencyId.FIREBALL_BOOST, p.getUniqueId(), 0);
        }
    }
}
