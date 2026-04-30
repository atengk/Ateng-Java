package io.github.atengk.diff;

import io.github.atengk.utils.diff.DiffUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MapDiffUtilTest {
    @Test
    void shouldDiffMapValues() {
        Map<String, Integer> oldMap = Map.of("a", 1, "b", 2);
        Map<String, Integer> newMap = Map.of("b", 3, "c", 4);
        DiffUtil.MapDiff<String, Integer> diff = DiffUtil.diffMap(oldMap, newMap);
        assertEquals(Map.of("c", 4), diff.addedEntries());
        assertEquals(Map.of("a", 1), diff.removedEntries());
        assertEquals(Set.of("b"), DiffUtil.getChangedKeys(oldMap, newMap));
        assertEquals(Set.of("c"), DiffUtil.getAddedKeys(oldMap, newMap));
        assertEquals(Set.of("a"), DiffUtil.getRemovedKeys(oldMap, newMap));
        assertTrue(DiffUtil.hasKeyChanged(oldMap, newMap, "b"));
        assertTrue(DiffUtil.hasMapChanged(oldMap, newMap));
    }

    @Test
    void shouldHandleEmptyAndUnchangedMap() {
        assertFalse(DiffUtil.hasMapChanged(Map.of("a", 1), Map.of("a", 1)));
        assertEquals(Set.of("a"), DiffUtil.getUnchangedKeys(Map.of("a", 1), Map.of("a", 1)));
        assertEquals(Map.of("a", 1), DiffUtil.getAddedEntries(Map.of(), Map.of("a", 1)));
        assertEquals(Map.of("a", 1), DiffUtil.getRemovedEntries(Map.of("a", 1), Map.of()));
        assertTrue(DiffUtil.getChangedEntries(Map.of("a", 1), Map.of("a", 2)).containsKey("a"));
    }
}
