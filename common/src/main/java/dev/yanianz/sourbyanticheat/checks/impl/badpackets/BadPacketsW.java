package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.player.SacPlayer;

@CheckData(name = "BadPacketsW", stableKey = "sac.badpackets.invalid_entity_target", description = "Interacted with non-existent entity")
public class BadPacketsW extends Check {
    public BadPacketsW(SacPlayer player) {
        super(player);
    }
}
