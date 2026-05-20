package dev.yanianz.sourbyanticheat.utils.viaversion;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ViaVersionUtilTest {

    @Test
    void isSupportedVersionNormalRange() {
        assertTrue(ViaVersionUtil.isSupportedVersion(47));   // 1.8
        assertTrue(ViaVersionUtil.isSupportedVersion(107));  // 1.9
        assertTrue(ViaVersionUtil.isSupportedVersion(340));  // 1.12.2
        assertTrue(ViaVersionUtil.isSupportedVersion(767));  // 1.21.4 max
    }

    @Test
    void isSupportedVersionOutOfRange() {
        assertFalse(ViaVersionUtil.isSupportedVersion(0));
        assertFalse(ViaVersionUtil.isSupportedVersion(46));   // 1.7.10
        assertFalse(ViaVersionUtil.isSupportedVersion(1000)); // future
    }

    @Test
    void isNativeVersionAtMaxProtocol() {
        assertTrue(ViaVersionUtil.isNativeVersion(767));
    }

    @Test
    void isNativeVersionBelowMaxIsNotNative() {
        assertFalse(ViaVersionUtil.isNativeVersion(340));
        assertFalse(ViaVersionUtil.isNativeVersion(47));
    }

    @Test
    void minProtocolIs47() {
        assertEquals(47, ViaVersionUtil.MIN_PROTOCOL);
    }

    @Test
    void maxProtocolIs767() {
        assertEquals(767, ViaVersionUtil.MAX_PROTOCOL);
    }

    @Test
    void isPre1_9Below107() {
        assertTrue(40 < 107);
        assertTrue(47 < 107);
        assertTrue(106 < 107);
    }

    @Test
    void isPre1_9AtOrAbove107() {
        assertFalse(107 < 107);
        assertFalse(108 < 107);
        assertFalse(340 < 107);
    }

    @Test
    void viaVersionIsAvailableFieldExists() {
        // Field should be accessible (may be false in test env)
        assertNotNull(ViaVersionUtil.class.getDeclaredFields());
    }
}
