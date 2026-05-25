package dev.yanianz.sourbyanticheat.profile.leniency.handlers;

import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyEventBus;
import dev.yanianz.sourbyanticheat.profile.leniency.LeniencyId;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

public final class KitPotionApplyHandler implements Listener {
    private static final Set<PotionEffectType> RELEVANT = Set.of(
            PotionEffectType.SPEED, PotionEffectType.JUMP_BOOST, PotionEffectType.LEVITATION);

    private final LeniencyEventBus bus;
    public KitPotionApplyHandler(LeniencyEventBus bus) { this.bus = bus; }

    @EventHandler
    public void onPotion(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.getAction() != EntityPotionEffectEvent.Action.ADDED) return;
        var effect = e.getNewEffect();
        if (effect == null) return;
        if (!RELEVANT.contains(effect.getType())) return;
        bus.fire(LeniencyId.KIT_POTION_APPLY, p.getUniqueId(), effect.getAmplifier());
    }
}
