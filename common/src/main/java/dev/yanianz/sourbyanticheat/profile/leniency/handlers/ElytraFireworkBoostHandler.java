package dev.yanianz.sourbyanticheat.profile.leniency.handlers;

import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyEventBus;
import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyId;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;

public final class ElytraFireworkBoostHandler implements Listener {
    private final LeniencyEventBus bus;
    public ElytraFireworkBoostHandler(LeniencyEventBus bus) { this.bus = bus; }

    @EventHandler
    public void onBoost(PlayerElytraBoostEvent e) {
        bus.fire(LeniencyId.ELYTRA_FIREWORK_BOOST, e.getPlayer().getUniqueId(), 0);
    }
}
