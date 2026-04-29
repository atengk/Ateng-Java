package io.github.atengk.object;

import io.github.atengk.utils.ObjectUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilEmptyTest {

    @Test
    void shouldCheckEmptyValues() {
        assertTrue(ObjectUtil.isEmpty(null));
        assertTrue(ObjectUtil.isEmpty(""));
        assertTrue(ObjectUtil.isEmpty(List.of()));
        assertTrue(ObjectUtil.isEmpty(Map.of()));
        assertTrue(ObjectUtil.isEmpty(new int[]{}));
        assertTrue(ObjectUtil.isEmpty(Optional.empty()));
        assertFalse(ObjectUtil.isEmpty(" "));
        assertFalse(ObjectUtil.isEmpty(List.of("a")));
        assertFalse(ObjectUtil.isEmpty(new int[]{1}));
    }

    @Test
    void shouldCheckBatchEmpty() {
        assertTrue(ObjectUtil.isAnyEmpty("a", ""));
        assertTrue(ObjectUtil.isAllEmpty(null, "", List.of()));
        assertTrue(ObjectUtil.isNoneEmpty("a", List.of("b")));
        assertEquals(3, ObjectUtil.emptyCount(null, "", List.of(), "a"));
        assertEquals(1, ObjectUtil.nonEmptyCount(null, "", List.of(), "a"));
    }

    @Test
    void shouldHandleEmptyVarargs() {
        assertFalse(ObjectUtil.isAnyEmpty());
        assertFalse(ObjectUtil.isAllEmpty());
        assertTrue(ObjectUtil.isNoneEmpty());
    }
}
