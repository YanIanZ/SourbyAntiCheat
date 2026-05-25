package dev.yanianz.sourbyanticheat.profile;

import org.junit.jupiter.api.Test;
import java.util.LinkedHashMap;
import static org.junit.jupiter.api.Assertions.*;

class ProfileWorldMapTest {
    @Test void exactMatch() {
        var map = new LinkedHashMap<String, Profile>();
        map.put("lobby", Profile.LOBBY);
        var m = new ProfileWorldMap(map, Profile.GENERIC);
        assertEquals(Profile.LOBBY, m.lookup("lobby"));
    }
    @Test void globMatch() {
        var map = new LinkedHashMap<String, Profile>();
        map.put("bedwars-*", Profile.BEDWARS);
        var m = new ProfileWorldMap(map, Profile.GENERIC);
        assertEquals(Profile.BEDWARS, m.lookup("bedwars-arena-42"));
    }
    @Test void firstMatchWins() {
        var map = new LinkedHashMap<String, Profile>();
        map.put("sky*", Profile.SKYWARS);
        map.put("skyblock-*", Profile.SKYBLOCK);
        var m = new ProfileWorldMap(map, Profile.GENERIC);
        assertEquals(Profile.SKYWARS, m.lookup("skyblock-1"));
    }
    @Test void noMatchFallsBackToDefault() {
        var map = new LinkedHashMap<String, Profile>();
        map.put("lobby", Profile.LOBBY);
        var m = new ProfileWorldMap(map, Profile.GENERIC);
        assertEquals(Profile.GENERIC, m.lookup("random-world"));
    }
    @Test void nullWorldFallsBackToDefault() {
        var m = new ProfileWorldMap(new LinkedHashMap<>(), Profile.PRACTICE);
        assertEquals(Profile.PRACTICE, m.lookup(null));
    }
}
