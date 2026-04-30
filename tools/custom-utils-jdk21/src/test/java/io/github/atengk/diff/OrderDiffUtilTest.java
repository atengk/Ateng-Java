package io.github.atengk.diff;

import io.github.atengk.utils.diff.DiffUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderDiffUtilTest {
    @Test
    void shouldDiffOrder() {
        assertTrue(DiffUtil.hasOrderChanged(List.of(1, 2, 3), List.of(3, 2, 1)));
        assertTrue(DiffUtil.diffOrder(List.of(1, 2, 3), List.of(3, 2, 1)).hasDiff());
        assertEquals(2, DiffUtil.getMovedItems(List.of(1, 2, 3), List.of(3, 2, 1)).size());
        assertTrue(DiffUtil.isSameElementsIgnoreOrder(List.of(1, 2), List.of(2, 1)));
        assertFalse(DiffUtil.isSameElementsWithOrder(List.of(1, 2), List.of(2, 1)));
    }

    @Test
    void shouldDiffOrderByKey() {
        ItemData a = new ItemData(1L, "A", 1);
        ItemData b = new ItemData(2L, "B", 2);
        assertTrue(DiffUtil.diffOrderByKey(List.of(a, b), List.of(b, a), ItemData::id).hasDiff());
        assertEquals(2, DiffUtil.getMovedItemsByKey(List.of(a, b), List.of(b, a), ItemData::id).size());
        assertThrows(NullPointerException.class, () -> DiffUtil.getMovedItemsByKey(List.of(a), List.of(a), null));
    }
}
