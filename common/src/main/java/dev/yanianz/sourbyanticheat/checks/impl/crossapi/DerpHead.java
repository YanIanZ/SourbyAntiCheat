package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.Check;
import dev.yanianz.sourbyanticheat.checks.CheckData;
import dev.yanianz.sourbyanticheat.checks.type.RotationCheck;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.spartan.SpartanCrossCheck;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.RotationUpdate;

@CheckData(name = "DerpHead", configName = "derphead", decay = 0.02, setback = 5, stableKey = "cross.derp")
public class DerpHead extends Check implements RotationCheck {

    private int derpTicks;
    private int buffer;

    // Config-wired threshold. Legit looking-down peaks around pitch 80-90, so the
    // unnatural-pitch bound sits near ±90 (true derp pegs the pitch fully).
    private double pitchThreshold = 89.5;

    public DerpHead(SacPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        pitchThreshold = config.getDoubleElse(getConfigName() + ".pitch-threshold", 89.5);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (player.disableGrim) return;

        if (player.inVehicle() || player.compensatedEntities.self.isDead
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || rotationUpdate.isCinematic()) return;

        float pitch = player.pitch;
        boolean unnaturalPitch = Math.abs(pitch) > pitchThreshold;

        if (unnaturalPitch) {
            derpTicks++;
        } else {
            derpTicks = Math.max(0, derpTicks - 2);
            buffer = Math.max(0, buffer - 1);
            reward();
            return;
        }

        if (derpTicks < 20) return;

        boolean nettyConfirms = player.crossValidationData.nettyIntervalVariance < 10.0;

        SpartanCrossCheck.CrossCheckResult spartanResult =
            SpartanCrossCheck.checkSpartan(player.uuid, "IrregularMovements");
        boolean spartanConfirms = spartanResult.type() == SpartanCrossCheck.CrossCheckResult.Type.SPARTAN_FLAGGED;

        buffer += (nettyConfirms || spartanConfirms) ? 2 : 1;
        if (buffer > 4) {
            flagAndAlert(String.format("pitch=%.1f ticks=%d nettyVar=%.1f spartan=%s",
                pitch, derpTicks,
                player.crossValidationData.nettyIntervalVariance, spartanResult.type()));
        }
    }
}
