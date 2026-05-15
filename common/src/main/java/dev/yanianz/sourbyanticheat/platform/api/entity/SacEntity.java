package dev.yanianz.sourbyanticheat.platform.api.entity;

import ac.grim.grimac.api.GrimIdentity;
import dev.yanianz.sourbyanticheat.platform.api.world.PlatformWorld;
import dev.yanianz.sourbyanticheat.utils.math.Location;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface SacEntity extends GrimIdentity {
    /**
     * Eject any passenger.
     *
     * @return True if there was a passenger.
     */
    boolean eject();

    CompletableFuture<Boolean> teleportAsync(Location location);

    @NotNull
    Object getNative();

    boolean isDead();

    PlatformWorld getWorld();

    Location getLocation();

    double distanceSquared(double x, double y, double z);
}
