package dev.yanianz.sourbyanticheat.checks.impl.combat;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.player.SacPlayer;

@CheckData(name = "Hitboxes", stableKey = "sac.combat.hitboxes", setback = 10)
public class Hitboxes extends Check {
    public Hitboxes(SacPlayer player) {
        super(player);
    }
}
