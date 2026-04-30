package io.github.atengk.diff;

import io.github.atengk.utils.diff.DiffUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BasicDiffUtilTest {
    @Test
    void shouldCheckBasicDiffStatus() {
        assertTrue(DiffUtil.isSame("A", "A"));
        assertFalse(DiffUtil.isDifferent(1, 1));
        assertTrue(DiffUtil.hasDiff(null, "A"));
        assertTrue(DiffUtil.hasNoDiff(null, null));
        assertEquals("B", DiffUtil.firstChanged("A", "B"));
        assertEquals("default", DiffUtil.defaultIfSame("A", "A", "default"));
        assertEquals("default", DiffUtil.defaultIfDifferent("A", "B", "default"));
    }

    @Test
    void shouldHandleBoundaryNullValues() {
        assertTrue(DiffUtil.isUnchanged(null, null));
        assertTrue(DiffUtil.isChanged(null, 1));
    }
}
