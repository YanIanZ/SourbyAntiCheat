package dev.yanianz.sourbyanticheat.checks.impl.misc;

import ac.grim.grimac.api.config.ConfigManager;
import dev.yanianz.sourbyanticheat.checks.type.BlockPlaceCheck;
import dev.yanianz.sourbyanticheat.platform.api.world.PlatformWorld;
import dev.yanianz.sourbyanticheat.player.SacPlayer;
import dev.yanianz.sourbyanticheat.utils.anticheat.update.BlockPlace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;

public class GhostBlockMitigation extends BlockPlaceCheck {

    private boolean allow;
    private int distance;

    public GhostBlockMitigation(SacPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (allow || player.platformPlayer == null) return;

        PlatformWorld world = player.platformPlayer.getWorld();
        Vector3i pos = place.getPlacedBlockPos();
        Vector3i posAgainst = place.position;

        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        int xAgainst = posAgainst.getX();
        int yAgainst = posAgainst.getY();
        int zAgainst = posAgainst.getZ();

        try {
            for (int i = x - distance; i <= x + distance; i++) {
                for (int j = y - distance; j <= y + distance; j++) {
                    for (int k = z - distance; k <= z + distance; k++) {
                        if (i == x && j == y && k == z) {
                            continue;
                        }

                        if (!world.isChunkLoaded(i >> 4, k >> 4)) {
                            continue;
                        }

                        WrappedBlockState type = world.getBlockAt(i, j, k);

                        if (!type.getType().isAir()) {
                            return;
                        }
                    }
                }
            }

            place.resync();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onReload(ConfigManager config) {
        allow = config.getBooleanElse("exploit.allow-building-on-ghostblocks", true);
        distance = config.getIntElse("exploit.distance-to-check-for-ghostblocks", 2);

        if (distance < 2 || distance > 4) distance = 2;
    }
}
