package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.player.SacPlayer;

@CheckData(name = "BadPacketsN", stableKey = "sac.badpackets.invalid_teleport", setback = 0)
public class BadPacketsN extends Check {
    public BadPacketsN(final SacPlayer player) {
        super(player);
    }
}
