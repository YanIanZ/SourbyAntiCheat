package dev.yanianz.sourbyanticheat.checks.impl.badpackets;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.player.SacPlayer;

/**
 * Detects interacting with a non-existent entity.
 *
 * This check has no packet listener by design — it is driven externally: the attack
 * packet handler ({@code PacketPlayerAttack}) calls {@code flagAndAlert()} on this
 * check when it resolves an attack against an entity that does not exist. The empty
 * class body is therefore intentional, not dead code.
 */
@CheckData(name = "BadPacketsW", stableKey = "sac.badpackets.invalid_entity_target", description = "Interacted with non-existent entity")
public class BadPacketsW extends Check {
    public BadPacketsW(SacPlayer player) {
        super(player);
    }
}
