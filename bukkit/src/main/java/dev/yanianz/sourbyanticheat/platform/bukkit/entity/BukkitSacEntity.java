package dev.yanianz.sourbyanticheat.platform.bukkit.entity;

import dev.yanianz.sourbyanticheat.platform.api.entity.SacEntity;
import dev.yanianz.sourbyanticheat.platform.api.world.PlatformWorld;
import dev.yanianz.sourbyanticheat.platform.bukkit.utils.convert.BukkitConversionUtils;
import dev.yanianz.sourbyanticheat.platform.bukkit.utils.reflection.PaperUtils;
import dev.yanianz.sourbyanticheat.platform.bukkit.world.BukkitPlatformWorld;
import dev.yanianz.sourbyanticheat.utils.math.Location;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BukkitSacEntity implements SacEntity {

    protected static final boolean CAN_USE_DIRECT_GETTERS = PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_20_1) && PaperUtils.PAPER;

    private final Entity entity;
    private BukkitPlatformWorld bukkitPlatformWorld;

    public BukkitSacEntity(Entity entity) {
        this.entity = Objects.requireNonNull(entity);
    }

    public Entity getBukkitEntity() {
        return this.entity;
    }

    @Override
    public UUID getUniqueId() {
        return entity.getUniqueId();
    }

    @Override
    public boolean eject() {
        return entity.eject();
    }

    @Override
    public CompletableFuture<Boolean> teleportAsync(Location location) {
        org.bukkit.Location bLoc = BukkitConversionUtils.toBukkitLocation(location);
        return PaperUtils.teleportAsync(this.entity, bLoc);
    }

    @Override
    @NotNull
    public Entity getNative() {
        return entity;
    }

    @Override
    public boolean isDead() {
        return this.entity.isDead();
    }

    // TODO replace with PlayerWorldChangeEvent listener instead of checking for equality for better performance
    @Override
    public PlatformWorld getWorld() {
        if (bukkitPlatformWorld == null || !bukkitPlatformWorld.bukkitWorld().equals(entity.getWorld())) {
            bukkitPlatformWorld = new BukkitPlatformWorld(entity.getWorld());
        }

        return bukkitPlatformWorld;
    }

    @Override
    public Location getLocation() {
        org.bukkit.Location location = this.entity.getLocation();
        return new Location(
                this.getWorld(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    @Override
    public double distanceSquared(double oX, double oY, double oZ) {
        if (CAN_USE_DIRECT_GETTERS) {
            double x = this.entity.getX();
            double y = this.entity.getY();
            double z = this.entity.getZ();
            double distX = (x - oX) * (x - oX);
            double distY = (y - oY) * (y - oY);
            double distZ = (z - oZ) * (z - oZ);
            return distX + distY + distZ;
        } else {
            org.bukkit.Location location = this.entity.getLocation();
            double x = location.getX();
            double y = location.getY();
            double z = location.getZ();
            double distX = (x - oX) * (x - oX);
            double distY = (y - oY) * (y - oY);
            double distZ = (z - oZ) * (z - oZ);
            return distX + distY + distZ;
        }
    }
}
