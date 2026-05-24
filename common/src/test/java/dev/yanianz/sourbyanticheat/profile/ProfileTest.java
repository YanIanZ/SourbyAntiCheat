package dev.yanianz.sourbyanticheat.profile;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProfileTest {
    @Test void fromStringCaseInsensitive() {
        assertEquals(Profile.BEDWARS, Profile.fromString("bedwars"));
        assertEquals(Profile.SKYWARS, Profile.fromString("SkyWars"));
        assertEquals(Profile.SKYBLOCK, Profile.fromString("SKYBLOCK"));
    }
    @Test void fromStringUnknownReturnsGeneric() {
        assertEquals(Profile.GENERIC, Profile.fromString("unknown"));
        assertEquals(Profile.GENERIC, Profile.fromString(""));
        assertEquals(Profile.GENERIC, Profile.fromString(null));
    }
    @Test void allSixValuesPresent() {
        assertEquals(6, Profile.values().length);
    }
}
