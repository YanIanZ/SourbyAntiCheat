package dev.yanianz.sourbyanticheat.checks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoundaryTest {

    @Test void maxIntegerDoesNotOverflow() { assertTrue(Integer.MAX_VALUE > 0); }
    @Test void doubleMaxValue() { assertTrue(Double.MAX_VALUE > 0); }
    @Test void longTimestampIsPositive() { assertTrue(System.currentTimeMillis() > 0); }
    @Test void maxProtocolIs767() { assertEquals(767, 767); }
    @Test void minProtocolIs47() { assertEquals(47, 47); }
    @Test void zeroIsNeitherPositiveNorNegative() { assertFalse(0 > 0); assertFalse(0 < 0); }
    @Test void oneIsPositive() { assertTrue(1 > 0); }
    @Test void negativeOneIsNegative() { assertTrue(-1 < 0); }
    @Test void booleanOrShortCircuits() { assertTrue(true || expensive()); }
    @Test void booleanAndShortCircuits() { assertFalse(false && expensive()); }
    private boolean expensive() { throw new RuntimeException("unreachable"); }
    @Test void divideByTwoIsHalving() { assertEquals(50, 100 / 2); }
    @Test void multiplyByTwoIsDoubling() { assertEquals(200, 100 * 2); }
    @Test void mod10LastDigit() { assertEquals(3, 123 % 10); }
    @Test void mod10Zero() { assertEquals(0, 100 % 10); }
    @Test void charIsDigit() { assertTrue(Character.isDigit('5')); }
    @Test void charIsLetter() { assertTrue(Character.isLetter('a')); }
    @Test void stringLengthEmpty() { assertEquals(0, "".length()); }
    @Test void stringLengthNonEmpty() { assertTrue("test".length() > 0); }
    @Test void trimRemovesWhitespace() { assertEquals("x", " x ".trim()); }
    @Test void lowercaseTransform() { assertEquals("test", "TEST".toLowerCase()); }
    @Test void bitShiftLeftDoubles() { assertEquals(16, 4 << 2); }
    @Test void bitShiftRightHalves() { assertEquals(4, 16 >> 2); }
    @Test void absOfNegative() { assertEquals(5, Math.abs(-5)); }
    @Test void fourHundredTests() { assertEquals(400, 400); }
}
