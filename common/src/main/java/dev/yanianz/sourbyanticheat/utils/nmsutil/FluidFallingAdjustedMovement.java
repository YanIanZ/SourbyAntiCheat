package dev.yanianz.sourbyanticheat.utils.nmsutil;

import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class FluidFallingAdjustedMovement {
    public static Vector3dm getFluidFallingAdjustedMovement(@NotNull SacPlayer player, double gravity, boolean isFalling, Vector3dm velocity) {
        if (!player.hasGravity || player.isSprinting) return velocity;
        isFalling = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14) ? isFalling : velocity.getY() < 0;
        double newY = isFalling && Math.abs(velocity.getY() - 0.005) >= 0.003 && Math.abs(velocity.getY() - gravity / 16.0) < 0.003 ? -0.003 : velocity.getY() - gravity / 16.0;
        return new Vector3dm(velocity.getX(), newY, velocity.getZ());
    }
}
