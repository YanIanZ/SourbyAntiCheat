package me.vagdedes.spartan.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Built-in SpartanAPI compatibility event.
 * Fired by SAC when a violation is detected, allowing plugins that listen
 * for Spartan's PlayerViolationEvent to receive SAC violations seamlessly.
 *
 * @author YanIanZ
 */
public class PlayerViolationEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player player;
    private final String hackType;
    private final int violations;
    private final String message;
    private boolean cancelled;

    public PlayerViolationEvent(Player player, String hackType, int violations, String message) {
        this.player = player;
        this.hackType = hackType;
        this.violations = violations;
        this.message = message;
        this.cancelled = false;
    }

    public Player getPlayer() {
        return player;
    }

    public String getHackType() {
        return hackType;
    }

    public int getViolations() {
        return violations;
    }

    public String getMessage() {
        return message;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
