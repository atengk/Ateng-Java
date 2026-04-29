package io.github.atengk.object;

import io.github.atengk.utils.ObjectUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilArrayTest {

    @Test
    void shouldCheckArray() {
        assertTrue(ObjectUtil.isArray(new int[]{1}));
        assertFalse(ObjectUtil.isArray("a"));
        assertEquals(2, ObjectUtil.arrayLength(new int[]{1, 2}));
        assertEquals(0, ObjectUtil.arrayLength(null));
        assertTrue(ObjectUtil.isEmptyArray(new String[]{}));
    }

    @Test
    void shouldCheckArrayContains() {
        String[] values = {"a", "b", null};
        assertTrue(ObjectUtil.contains(values, "a"));
        assertTrue(ObjectUtil.contains(values, null));
        assertTrue(ObjectUtil.containsAny(values, "x", "b"));
        assertTrue(ObjectUtil.containsAll(values, "a", "b"));
        assertFalse(ObjectUtil.containsAll(values, "a", "x"));
    }

    @Test
    void shouldThrowWhenNotArray() {
        assertThrows(IllegalArgumentException.class, () -> ObjectUtil.arrayLength("a"));
        assertThrows(IllegalArgumentException.class, () -> ObjectUtil.isEmptyArray("a"));
    }
}
