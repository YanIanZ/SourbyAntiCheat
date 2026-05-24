package dev.yanianz.sourbyanticheat.profile;

import java.util.UUID;
import java.util.function.Function;

public final class ProfileResolver {
    private static final Profile[] PERM_ORDER = {
            Profile.BEDWARS, Profile.SKYWARS, Profile.SKYBLOCK,
            Profile.PRACTICE, Profile.LOBBY, Profile.GENERIC
    };

    private final Function<UUID, String> worldOf;
    private final Function<String, Boolean> hasPermission;
    private final ProfileWorldMap worldMap;

    public ProfileResolver(Function<UUID, String> worldOf,
                           Function<String, Boolean> hasPermission,
                           ProfileWorldMap worldMap) {
        this.worldOf = worldOf;
        this.hasPermission = hasPermission;
        this.worldMap = worldMap;
    }

    public Profile resolve(UUID playerId) {
        for (Profile p : PERM_ORDER) {
            if (hasPermission.apply("sac.profile." + p.name().toLowerCase())) return p;
        }
        return worldMap.lookup(worldOf.apply(playerId));
    }
}
