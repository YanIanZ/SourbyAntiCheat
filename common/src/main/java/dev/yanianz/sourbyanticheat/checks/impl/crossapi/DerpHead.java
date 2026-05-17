package dev.yanianz.sourbyanticheat.checks.impl.crossapi;

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

    public DerpHead(SacPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (player.disableGrim) return;

        if (player.inVehicle() || player.compensatedEntities.self.isDead
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR
                || rotationUpdate.isCinematic()) return;

        float pitch = player.pitch;
        boolean unnaturalPitch = Math.abs(pitch) > 80;

        if (unnaturalPitch) {
            derpTicks++;
        } else {
            derpTicks = Math.max(0, derpTicks - 2);
            buffer = Math.max(0, buffer - 1);
            if (buffer < 2) reward();
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
