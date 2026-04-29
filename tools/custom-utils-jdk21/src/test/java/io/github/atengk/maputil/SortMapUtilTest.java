package io.github.atengk.maputil;

import io.github.atengk.utils.MapUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class SortMapUtilTest {

    @Test
    void shouldSortByKeyAndValue() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("b", 2);
        map.put("a", 3);
        map.put("c", 1);

        assertEquals(List.of("a", "b", "c"), new ArrayList<>(MapUtil.sortByKey(map).keySet()));
        assertEquals(List.of("c", "b", "a"), new ArrayList<>(MapUtil.sortByValue(map).keySet()));
        assertEquals(List.of("c", "b", "a"), new ArrayList<>(MapUtil.sortByKey(map, (x, y) -> y.compareTo(x)).keySet()));
        assertThrows(NullPointerException.class, () -> MapUtil.sortByKey(map, null));
    }

    @Test
    void shouldConvertToTreeLinkedAndReverse() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("b", 2);
        map.put("a", 1);

        assertInstanceOf(TreeMap.class, MapUtil.toTreeMap(map));
        assertInstanceOf(LinkedHashMap.class, MapUtil.toLinkedHashMap(map));
        assertEquals(List.of("a", "b"), new ArrayList<>(MapUtil.toTreeMap(map).keySet()));
        assertEquals(List.of("a", "b"), new ArrayList<>(MapUtil.reverse(map).keySet()));
    }
}
