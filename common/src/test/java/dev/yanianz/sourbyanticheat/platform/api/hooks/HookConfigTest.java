package dev.yanianz.sourbyanticheat.platform.api.hooks;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HookConfigTest {

    @Test
    void disabledPluginYieldsNoChecks() {
        HookConfig c = new HookConfig(false, Map.of("berserk", List.of("FastBreak")));
        assertFalse(c.enabled());
        assertTrue(c.checksFor("berserk").isEmpty());
    }

    @Test
    void enabledReturnsMappedChecks() {
        HookConfig c = new HookConfig(true, Map.of(
                "berserk", List.of("FastBreak"),
                "super_breaker", List.of("FastBreak")));
        assertTrue(c.enabled());
        assertEquals(List.of("FastBreak"), c.checksFor("berserk"));
        assertEquals(List.of("FastBreak"), c.checksFor("super_breaker"));
    }

    @Test
    void unknownAbilityYieldsEmpty() {
        HookConfig c = new HookConfig(true, Map.of("berserk", List.of("FastBreak")));
        assertTrue(c.checksFor("nonexistent").isEmpty());
    }

    @Test
    void keysAreCaseInsensitive() {
        HookConfig c = new HookConfig(true, Map.of("berserk", List.of("FastBreak")));
        assertEquals(List.of("FastBreak"), c.checksFor("BERSERK"));
    }
}
