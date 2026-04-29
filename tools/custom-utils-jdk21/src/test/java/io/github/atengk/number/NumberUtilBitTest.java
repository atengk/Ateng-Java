package io.github.atengk.number;

import io.github.atengk.utils.NumberUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NumberUtilBitTest {

    @Test
    void shouldHandlePowerOfTwo() {
        assertTrue(NumberUtil.isPowerOfTwo(8));
        assertFalse(NumberUtil.isPowerOfTwo(0));
        assertEquals(8L, NumberUtil.nextPowerOfTwo(5));
        assertEquals(8L, NumberUtil.nextPowerOfTwo(8));
    }

    @Test
    void shouldHandleBitOperations() {
        assertEquals(2, NumberUtil.bitCount(3));
        assertTrue(NumberUtil.hasBit(2, 1));
        assertEquals(2L, NumberUtil.setBit(0, 1));
        assertEquals(0L, NumberUtil.clearBit(2, 1));
        assertEquals(0L, NumberUtil.toggleBit(2, 1));
        assertEquals("101", NumberUtil.toBinaryString(5));
        assertEquals("ff", NumberUtil.toHexString(255));
    }

    @Test
    void shouldThrowForInvalidBitArguments() {
        assertThrows(IllegalArgumentException.class, () -> NumberUtil.hasBit(1, -1));
        assertThrows(IllegalArgumentException.class, () -> NumberUtil.nextPowerOfTwo(Long.MAX_VALUE));
    }
}
