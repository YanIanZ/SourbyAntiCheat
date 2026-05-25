package dev.yanianz.sourbyanticheat.profile;

import org.junit.jupiter.api.Test;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;

class ProfileResolverTest {
    UUID p = UUID.randomUUID();

    static ProfileResolver build(String world, Function<String, Boolean> hasPerm,
                                 LinkedHashMap<String, Profile> worldMap) {
        var map = new ProfileWorldMap(worldMap, Profile.GENERIC);
        return new ProfileResolver(uuid -> world, (uuid, perm) -> hasPerm.apply(perm), map);
    }

    @Test void permissionBeatsWorld() {
        var wm = new LinkedHashMap<String, Profile>();
        wm.put("arena", Profile.BEDWARS);
        var r = build("arena", perm -> perm.equals("sac.profile.skywars"), wm);
        assertEquals(Profile.SKYWARS, r.resolve(p));
    }
    @Test void worldWhenNoPerm() {
        var wm = new LinkedHashMap<String, Profile>();
        wm.put("arena", Profile.BEDWARS);
        var r = build("arena", perm -> false, wm);
        assertEquals(Profile.BEDWARS, r.resolve(p));
    }
    @Test void defaultWhenNoMatch() {
        var r = build("uncharted", perm -> false, new LinkedHashMap<>());
        assertEquals(Profile.GENERIC, r.resolve(p));
    }
    @Test void permissionPriorityOrder() {
        // bedwars > skywars > skyblock > practice > lobby > generic
        var r = build(null, perm ->
                perm.equals("sac.profile.skyblock") || perm.equals("sac.profile.practice"),
                new LinkedHashMap<>());
        assertEquals(Profile.SKYBLOCK, r.resolve(p));
    }
}
