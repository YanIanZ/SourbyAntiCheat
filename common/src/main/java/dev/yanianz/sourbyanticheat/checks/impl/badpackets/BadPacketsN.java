package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.player.SacPlayer;

/**
 * Detects a player ignoring a required setback teleport.
 *
 * <p>This check has no packet listener by design: it is flagged externally by
 * {@code SetbackTeleportUtil} when the player acknowledges past a pending teleport's
 * transaction without accepting it. It must therefore remain registered even though
 * the class body is empty.
 */
@CheckData(name = "BadPacketsN", stableKey = "sac.badpackets.invalid_teleport", setback = 0)
public class BadPacketsN extends Check {
    public BadPacketsN(final SacPlayer player) {
        super(player);
    }
}
