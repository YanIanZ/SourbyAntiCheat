package dev.yanianz.sourbyanticheat.platform.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlatformTest {

    @Test
    void resolveBukkitByName() {
        assertEquals(Platform.BUKKIT, Platform.resolveByName("bukkit"));
        assertEquals(Platform.BUKKIT, Platform.resolveByName("BUKKIT"));
    }

    @Test
    void resolveFoliaByName() {
        assertEquals(Platform.FOLIA, Platform.resolveByName("folia"));
        assertEquals(Platform.FOLIA, Platform.resolveByName("FOLIA"));
    }

    @Test
    void resolveVelocityByName() {
        assertEquals(Platform.VELOCITY, Platform.resolveByName("velocity"));
    }

    @Test
    void resolveBungeeByName() {
        assertEquals(Platform.BUNGEECORD, Platform.resolveByName("bungeecord"));
    }

    @Test
    void resolveTestByName() {
        assertEquals(Platform.TEST, Platform.resolveByName("test"));
    }

    @Test
    void resolveUnknownReturnsNull() {
        assertNull(Platform.resolveByName("spigot"));
        assertNull(Platform.resolveByName("unknown"));
        assertNull(Platform.resolveByName(""));
        assertNull(Platform.resolveByName(null));
    }

    @Test
    void allValuesIncludeTest() {
        boolean hasTest = false;
        for (Platform p : Platform.values()) {
            if (p == Platform.TEST) hasTest = true;
        }
        assertTrue(hasTest);
    }

    @Test
    void platformEnumHasFiveValues() {
        assertEquals(5, Platform.values().length);
    }
}
