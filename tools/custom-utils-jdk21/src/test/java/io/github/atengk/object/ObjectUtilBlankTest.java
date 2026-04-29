package io.github.atengk.object;

import io.github.atengk.utils.ObjectUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilBlankTest {

    @Test
    void shouldCheckBlankValues() {
        assertTrue(ObjectUtil.isBlank(null));
        assertTrue(ObjectUtil.isBlank(""));
        assertTrue(ObjectUtil.isBlank(" \t\n"));
        assertFalse(ObjectUtil.isBlank(" a "));
        assertFalse(ObjectUtil.isBlank(123));
        assertTrue(ObjectUtil.isNotBlank("a"));
    }

    @Test
    void shouldCheckBatchBlank() {
        assertTrue(ObjectUtil.isAnyBlank("a", " "));
        assertTrue(ObjectUtil.isAllBlank(null, "", " "));
        assertTrue(ObjectUtil.isNoneBlank("a", 123));
        assertFalse(ObjectUtil.isNoneBlank("a", null));
    }

    @Test
    void shouldHandleEmptyVarargs() {
        assertFalse(ObjectUtil.isAnyBlank());
        assertFalse(ObjectUtil.isAllBlank());
        assertTrue(ObjectUtil.isNoneBlank());
    }
}
