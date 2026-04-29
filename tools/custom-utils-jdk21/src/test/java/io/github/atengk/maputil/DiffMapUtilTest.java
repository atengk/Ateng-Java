package io.github.atengk.maputil;

import io.github.atengk.utils.MapUtil;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DiffMapUtilTest {

    @Test
    void shouldFindAddedRemovedAndChangedEntries() {
        Map<String, Integer> oldMap = new LinkedHashMap<>();
        oldMap.put("a", 1);
        oldMap.put("b", 2);
        oldMap.put("c", 3);
        Map<String, Integer> newMap = new LinkedHashMap<>();
        newMap.put("b", 20);
        newMap.put("c", 3);
        newMap.put("d", 4);

        assertEquals(Map.of("d", 4), MapUtil.added(oldMap, newMap));
        assertEquals(Map.of("a", 1), MapUtil.removed(oldMap, newMap));
        assertEquals(2, MapUtil.changed(oldMap, newMap).get("b").getOldValue());
        assertEquals(20, MapUtil.changed(oldMap, newMap).get("b").getNewValue());

        MapUtil.MapDiff<String, Integer> diff = MapUtil.diff(oldMap, newMap);
        assertEquals(Map.of("d", 4), diff.getAdded());
        assertEquals(Map.of("a", 1), diff.getRemoved());
        assertTrue(diff.getChanged().containsKey("b"));
    }

    @Test
    void shouldCompareMapAndValue() {
        Map<String, Integer> one = new LinkedHashMap<>();
        one.put("a", 1);
        one.put("b", 2);
        Map<String, Integer> two = new LinkedHashMap<>();
        two.put("b", 2);
        two.put("a", 1);

        assertTrue(MapUtil.same(one, two));
        assertTrue(MapUtil.equalsIgnoreOrder(one, two));
        assertTrue(MapUtil.compareValue(one, two, "a").isEmpty());
        assertTrue(MapUtil.compareValue(one, Map.of("a", 9), "a").isPresent());
        assertTrue(MapUtil.compareValue(null, Map.of("a", 1), "a").isPresent());
    }
}
