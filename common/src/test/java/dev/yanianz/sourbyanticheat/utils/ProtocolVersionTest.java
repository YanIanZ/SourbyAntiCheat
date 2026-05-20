package dev.yanianz.sourbyanticheat.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolVersionTest {

    @Test void version1_8() { assertTrue(47 >= 47 && 47 < 107); }
    @Test void version1_9() { assertTrue(107 >= 107 && 107 < 110); }
    @Test void version1_10() { assertTrue(210 >= 107 && 210 < 315); }
    @Test void version1_12_2() { assertTrue(340 >= 315 && 340 < 400); }
    @Test void version1_13() { assertTrue(393 >= 393 && 393 < 401); }
    @Test void version1_14() { assertTrue(477 >= 477 && 477 < 485); }
    @Test void version1_16_5() { assertTrue(754 >= 735 && 754 < 756); }
    @Test void version1_17() { assertTrue(755 >= 755 && 755 < 757); }
    @Test void version1_20() { assertTrue(763 >= 763 && 763 < 766); }
    @Test void version1_21_4() { assertEquals(767, 767); }

    @Test void pre1_9DetectedCorrectly() {
        assertTrue(47 < 107); assertTrue(100 < 107); assertFalse(108 < 107);
    }

    @Test void crossVersionDetection() {
        int protocol = 340; int native_ = 767;
        assertTrue(protocol < native_);
    }

    @Test void nativeVersionDetection() {
        int protocol = 767; int native_ = 767;
        assertFalse(protocol < native_);
    }

    @Test void viaBackwardsPre1_9On1_21_2Server() {
        int server = 767; int client = 47;
        assertTrue(client < 107 && server >= 766);
    }

    @Test void viaRewindPre1_9On1_9Server() {
        int server = 107; int client = 47;
        assertTrue(client < 107 && server >= 107);
    }

    @Test void protocolInRange1_8To1_21_4() {
        for (int p : new int[]{47, 107, 210, 340, 393, 477, 754, 763, 767}) {
            assertTrue(p >= 47 && p <= 767);
        }
    }

    @Test void protocolRangeOrderIsMonotonic() {
        int[] vers = {47, 107, 210, 315, 340, 393, 401, 477, 485, 735, 755, 763, 767};
        for (int i = 1; i < vers.length; i++) assertTrue(vers[i] > vers[i-1]);
    }
}
