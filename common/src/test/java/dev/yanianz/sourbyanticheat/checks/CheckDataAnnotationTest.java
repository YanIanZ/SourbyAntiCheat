package dev.yanianz.sourbyanticheat.checks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CheckDataAnnotationTest {

    @CheckData(name = "TestCheck", configName = "testcheck", decay = 0.05, setback = 10, stableKey = "test.check")
    static class TestCheck {}

    @Test void annotationHasName() {
        CheckData a = TestCheck.class.getAnnotation(CheckData.class);
        assertNotNull(a);
        assertEquals("TestCheck", a.name());
    }

    @Test void annotationHasConfigName() {
        assertEquals("testcheck", TestCheck.class.getAnnotation(CheckData.class).configName());
    }

    @Test void annotationHasDecay() {
        assertEquals(0.05, TestCheck.class.getAnnotation(CheckData.class).decay(), 0.001);
    }

    @Test void annotationHasSetback() {
        assertEquals(10, TestCheck.class.getAnnotation(CheckData.class).setback());
    }

    @Test void annotationDefaultAlternativeName() {
        assertEquals("UNKNOWN", TestCheck.class.getAnnotation(CheckData.class).alternativeName());
    }

    @Test void annotationDefaultDescription() {
        assertEquals("No description provided", TestCheck.class.getAnnotation(CheckData.class).description());
    }

    @Test void annotationHasStableKey() {
        assertEquals("test.check", TestCheck.class.getAnnotation(CheckData.class).stableKey());
    }

    @Test void annotationIsRuntimeRetained() {
        assertTrue(TestCheck.class.isAnnotationPresent(CheckData.class));
    }

    @Test void crossSpeedBAnnotationIsCorrect() throws Exception {
        Class<?> c = Class.forName("dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossSpeedB");
        CheckData a = c.getAnnotation(CheckData.class);
        assertNotNull(a);
        assertEquals("CrossSpeedB", a.name());
        assertEquals("crossspeedb", a.configName());
    }

    @Test void crossFastBreakBAnnotationIsCorrect() throws Exception {
        Class<?> c = Class.forName("dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossFastBreakB");
        CheckData a = c.getAnnotation(CheckData.class);
        assertEquals("CrossFastBreakB", a.name());
    }

    @Test void crossNoFallAnnotationIsCorrect() throws Exception {
        Class<?> c = Class.forName("dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossNoFall");
        CheckData a = c.getAnnotation(CheckData.class);
        assertEquals("CrossNoFall", a.name());
    }

    @Test void crossNoSwingAnnotationIsCorrect() throws Exception {
        Class<?> c = Class.forName("dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossNoSwing");
        CheckData a = c.getAnnotation(CheckData.class);
        assertEquals("CrossNoSwing", a.name());
    }

    @Test void crossAntiKBAnnotationIsCorrect() throws Exception {
        Class<?> c = Class.forName("dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossAntiKB");
        CheckData a = c.getAnnotation(CheckData.class);
        assertEquals("CrossAntiKB", a.name());
    }

    @Test void crossJesusAnnotationIsCorrect() throws Exception {
        Class<?> c = Class.forName("dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossJesus");
        CheckData a = c.getAnnotation(CheckData.class);
        assertEquals("CrossJesus", a.name());
    }

    @Test void crossSpeedAnnotationIsCorrect() throws Exception {
        Class<?> c = Class.forName("dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossSpeed");
        CheckData a = c.getAnnotation(CheckData.class);
        assertEquals("CrossSpeed", a.name());
    }

    @Test void crossFastBreakAnnotationIsCorrect() throws Exception {
        Class<?> c = Class.forName("dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossFastBreak");
        CheckData a = c.getAnnotation(CheckData.class);
        assertEquals("CrossFastBreak", a.name());
    }

    @Test void aimAssistAnnotationHasStableKey() throws Exception {
        Class<?> c = Class.forName("dev.yanianz.sourbyanticheat.checks.impl.aim.AimAssist");
        CheckData a = c.getAnnotation(CheckData.class);
        assertNotNull(a);
        assertFalse(a.stableKey().isEmpty());
    }

    @Test void allCrossChecksHaveStableKeys() throws Exception {
        String[] checkClasses = {
            "dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossSpeedB",
            "dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossFastBreakB",
            "dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossNoFall",
            "dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossNoSwing",
            "dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossAntiKB",
            "dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossJesus"
        };
        for (String cls : checkClasses) {
            Class<?> c = Class.forName(cls);
            CheckData a = c.getAnnotation(CheckData.class);
            assertNotNull(a, cls + " missing @CheckData");
            assertFalse(a.stableKey().isEmpty(), cls + " missing stableKey");
        }
    }

    @Test void decayValuesArePositive() throws Exception {
        String[] checks = {
            "dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossSpeedB",
            "dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossFastBreakB",
            "dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossNoFall",
            "dev.yanianz.sourbyanticheat.checks.impl.crossapi.CrossNoSwing"
        };
        for (String cls : checks) {
            CheckData a = Class.forName(cls).getAnnotation(CheckData.class);
            assertTrue(a.decay() > 0, cls + " decay <= 0");
        }
    }
}
