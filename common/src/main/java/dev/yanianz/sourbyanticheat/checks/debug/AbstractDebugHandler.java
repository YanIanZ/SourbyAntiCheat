package dev.yanianz.sourbyanticheat.checks.debug;

import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.player.SacPlayer;

public abstract class AbstractDebugHandler extends Check {
    public AbstractDebugHandler(SacPlayer player) {
        super(player);
    }

    public abstract void toggleListener(SacPlayer player);

    public abstract boolean toggleConsoleOutput();
}
