package dev.yanianz.sourbyanticheat.profile;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class ProfileWorldMapAddMappingTest {

    @Test
    void addedMappingIsMatched() {
        ProfileWorldMap map = new ProfileWorldMap(new LinkedHashMap<>(), Profile.GENERIC);
        assertEquals(Profile.GENERIC, map.lookup("SuperiorWorld"));
        map.addMapping("SuperiorWorld*", Profile.SKYBLOCK);
        assertEquals(Profile.SKYBLOCK, map.lookup("SuperiorWorld"));
        assertEquals(Profile.SKYBLOCK, map.lookup("SuperiorWorld_nether"));
        assertEquals(Profile.GENERIC, map.lookup("lobby"));
    }

    @Test
    void fileMappingsTakePrecedenceOverLaterAdds() {
        LinkedHashMap<String, Profile> raw = new LinkedHashMap<>();
        raw.put("arena", Profile.BEDWARS);
        ProfileWorldMap map = new ProfileWorldMap(raw, Profile.GENERIC);
        map.addMapping("arena", Profile.SKYBLOCK); // later add must NOT override existing first-match
        assertEquals(Profile.BEDWARS, map.lookup("arena"));
    }
}
