package io.github.atengk.object;

import io.github.atengk.utils.ObjectUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilNullTest {

    @Test
    void shouldCheckSingleNull() {
        assertTrue(ObjectUtil.isNull(null));
        assertFalse(ObjectUtil.isNull("a"));
        assertTrue(ObjectUtil.isNotNull("a"));
        assertFalse(ObjectUtil.isNotNull(null));
    }

    @Test
    void shouldCheckBatchNull() {
        assertTrue(ObjectUtil.isAnyNull("a", null, 1));
        assertFalse(ObjectUtil.isAnyNull("a", 1));
        assertTrue(ObjectUtil.isAllNull(null, null));
        assertFalse(ObjectUtil.isAllNull(null, "a"));
        assertTrue(ObjectUtil.isNoneNull("a", 1));
        assertFalse(ObjectUtil.isNoneNull("a", null));
    }

    @Test
    void shouldCountNullValues() {
        assertEquals(2, ObjectUtil.nullCount(null, "a", null));
        assertEquals(1, ObjectUtil.nonNullCount(null, "a", null));
        assertEquals(0, ObjectUtil.nullCount((Object[]) null));
        assertEquals(0, ObjectUtil.nonNullCount((Object[]) null));
    }

    @Test
    void shouldHandleEmptyVarargs() {
        assertFalse(ObjectUtil.isAnyNull());
        assertFalse(ObjectUtil.isAllNull());
        assertTrue(ObjectUtil.isNoneNull());
    }
}
