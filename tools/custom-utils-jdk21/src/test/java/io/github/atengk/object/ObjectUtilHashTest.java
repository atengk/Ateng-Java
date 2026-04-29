package io.github.atengk.object;

import io.github.atengk.utils.ObjectUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilHashTest {

    @Test
    void shouldCalculateHash() {
        assertEquals(0, ObjectUtil.hashCode(null));
        assertEquals("a".hashCode(), ObjectUtil.hashCode("a"));
        assertEquals(Objects.hash("a", 1), ObjectUtil.hash("a", 1));
    }

    @Test
    void shouldCalculateDeepHash() {
        assertEquals(Arrays.hashCode(new int[]{1, 2}), ObjectUtil.deepHashCode(new int[]{1, 2}));
        assertEquals(Arrays.deepHashCode(new Object[]{new int[]{1}}), ObjectUtil.deepHashCode(new Object[]{new int[]{1}}));
        assertEquals(0, ObjectUtil.deepHashCode(null));
    }

    @Test
    void shouldCalculateIdentityHash() {
        Object value = new Object();
        assertEquals(System.identityHashCode(value), ObjectUtil.identityHashCode(value));
        assertEquals(0, ObjectUtil.identityHashCode(null));
    }
}
