package dev.yanianz.sourbyanticheat.utils.data;

import dev.yanianz.sourbyanticheat.player.SacPlayer;

public class LastInstance {
    private int lastInstance = 100;

    public LastInstance(SacPlayer player) {
        player.lastInstanceManager.addInstance(this);
    }

    public boolean hasOccurredSince(int time) {
        return lastInstance <= time;
    }

    public void reset() {
        lastInstance = 0;
    }

    public void tick() {
        // Don't overflow (a VERY long timer attack or a player playing for days could cause this to overflow)
        // The CPU can predict this branch, so it's only a few cycles.
        if (lastInstance == Integer.MAX_VALUE) lastInstance = 100;
        lastInstance++;
    }
}
