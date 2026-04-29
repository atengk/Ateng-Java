package io.github.atengk.object;

import io.github.atengk.utils.ObjectUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilCompareTest {

    @Test
    void shouldCompareValues() {
        assertTrue(ObjectUtil.compare(1, 2) < 0);
        assertTrue(ObjectUtil.compare(null, 1) < 0);
        assertTrue(ObjectUtil.compare(null, 1, true) > 0);
        assertEquals(Integer.valueOf(1), ObjectUtil.min(1, 2));
        assertEquals(Integer.valueOf(2), ObjectUtil.max(1, 2));
    }

    @Test
    void shouldCheckRange() {
        assertTrue(ObjectUtil.between(2, 1, 3));
        assertTrue(ObjectUtil.between(1, 1, 3));
        assertFalse(ObjectUtil.between(null, 1, 3));
        assertTrue(ObjectUtil.notBetween(4, 1, 3));
        assertEquals(Integer.valueOf(1), ObjectUtil.clamp(0, 1, 3));
        assertEquals(Integer.valueOf(3), ObjectUtil.clamp(4, 1, 3));
        assertEquals(Integer.valueOf(2), ObjectUtil.clamp(2, 1, 3));
        assertNull(ObjectUtil.clamp(null, 1, 3));
    }

    @Test
    void shouldThrowWhenRangeInvalid() {
        assertThrows(IllegalArgumentException.class, () -> ObjectUtil.between(2, 3, 1));
        assertThrows(IllegalArgumentException.class, () -> ObjectUtil.clamp(2, 3, 1));
    }
}
