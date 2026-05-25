package dev.yanianz.sourbyanticheat.profile.leniency.handlers;

import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyEventBus;
import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyId;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class SnowballKbHandler implements Listener {
    private final LeniencyEventBus bus;
    public SnowballKbHandler(LeniencyEventBus bus) { this.bus = bus; }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!(e.getDamager() instanceof Snowball || e.getDamager() instanceof Egg)) return;
        bus.fire(LeniencyId.SNOWBALL_KB, p.getUniqueId(), 0);
    }
}
