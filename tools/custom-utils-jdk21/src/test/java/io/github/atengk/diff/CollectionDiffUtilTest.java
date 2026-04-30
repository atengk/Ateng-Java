package io.github.atengk.diff;

import io.github.atengk.utils.diff.DiffUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CollectionDiffUtilTest {
    @Test
    void shouldDiffCollectionValues() {
        DiffUtil.CollectionDiff<Integer> diff = DiffUtil.diffCollection(List.of(1, 2, 3), List.of(2, 3, 4));
        assertEquals(List.of(4), diff.added());
        assertEquals(List.of(1), diff.removed());
        assertEquals(List.of(2, 3), diff.unchanged());
        assertEquals(List.of(1, 4), DiffUtil.getChanged(List.of(1, 2), List.of(2, 4)));
        assertTrue(DiffUtil.hasAdded(List.of(1), List.of(1, 2)));
        assertTrue(DiffUtil.hasRemoved(List.of(1, 2), List.of(1)));
        assertTrue(DiffUtil.hasCollectionChanged(List.of(1), List.of(2)));
        assertEquals(Set.of(2), Set.copyOf(DiffUtil.diffSet(Set.of(1), Set.of(1, 2)).added()));
        assertEquals(List.of("b"), DiffUtil.diffArray(new String[]{"a"}, new String[]{"a", "b"}).added());
    }

    @Test
    void shouldDiffCollectionByKey() {
        ItemData oldItem = new ItemData(1L, "A", 1);
        ItemData newItem = new ItemData(1L, "B", 1);
        ItemData addItem = new ItemData(2L, "C", 2);
        DiffUtil.CollectionDiff<ItemData> diff = DiffUtil.diffListByKey(List.of(oldItem), List.of(newItem, addItem), ItemData::id);
        assertEquals(1, diff.added().size());
        assertEquals(1, diff.modified().size());
        assertEquals(1, DiffUtil.getAddedByKey(List.of(oldItem), List.of(newItem, addItem), ItemData::id).size());
        assertEquals(0, DiffUtil.getRemovedByKey(List.of(oldItem), List.of(newItem, addItem), ItemData::id).size());
        assertEquals(1, DiffUtil.getModifiedByKey(List.of(oldItem), List.of(newItem), ItemData::id).size());
        assertTrue(DiffUtil.diffElementByKey(List.of(oldItem), List.of(newItem), ItemData::id, (a, b) -> a.id().equals(b.id())).modified().isEmpty());
        assertTrue(DiffUtil.diffElementFieldsByKey(List.of(oldItem), List.of(newItem), ItemData::id).hasDiff());
    }

    @Test
    void shouldValidateKeyExtractor() {
        assertThrows(NullPointerException.class, () -> DiffUtil.diffCollectionByKey(List.of("a"), List.of("b"), null));
        assertThrows(IllegalArgumentException.class, () -> DiffUtil.diffCollectionByKey(List.of("a"), List.of("b"), value -> null));
    }
}
