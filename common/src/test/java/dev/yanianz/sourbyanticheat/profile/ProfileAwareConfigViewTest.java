package dev.yanianz.sourbyanticheat.profile;

import ac.grim.grimac.api.config.ConfigManager;
import org.junit.jupiter.api.Test;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileAwareConfigViewTest {
    @Test void returnsOverrideWhenProfileSpecifies() {
        var base = mock(ConfigManager.class);
        when(base.getDoubleElse("Reach.threshold", 99.0)).thenReturn(99.0);
        var section = new ProfileConfigSnapshot.ProfileSection(
                Set.of(),
                Map.of("Reach", Map.of("threshold", 3.3)),
                List.of());
        var view = new ProfileAwareConfigView(base, section);
        assertEquals(3.3, view.getDoubleElse("Reach.threshold", 99.0), 1e-9);
    }
    @Test void fallsToBaseWhenNoOverride() {
        var base = mock(ConfigManager.class);
        when(base.getDoubleElse("Reach.threshold", 99.0)).thenReturn(42.0);
        var section = new ProfileConfigSnapshot.ProfileSection(Set.of(), Map.of(), List.of());
        var view = new ProfileAwareConfigView(base, section);
        assertEquals(42.0, view.getDoubleElse("Reach.threshold", 99.0), 1e-9);
    }
    @Test void disabledCheckReportsFalseForEnabledFlag() {
        var base = mock(ConfigManager.class);
        when(base.getBooleanElse("checks.enabled.Reach", true)).thenReturn(true);
        var section = new ProfileConfigSnapshot.ProfileSection(Set.of("Reach"), Map.of(), List.of());
        var view = new ProfileAwareConfigView(base, section);
        assertFalse(view.getBooleanElse("checks.enabled.Reach", true));
    }
}
