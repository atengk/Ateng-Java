package io.github.atengk.maputil;

import io.github.atengk.utils.MapUtil;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TransformMapUtilTest {

    @Test
    void shouldMapKeysValuesAndEntries() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        assertEquals(Map.of("A", 1, "B", 2), MapUtil.mapKeys(map, String::toUpperCase));
        assertEquals(Map.of("a", 10, "b", 20), MapUtil.mapValues(map, value -> value * 10));
        assertEquals(Map.of("a1", "1", "b2", "2"), MapUtil.mapEntries(map, entry -> new AbstractMap.SimpleEntry<>(entry.getKey() + entry.getValue(), String.valueOf(entry.getValue()))));
        assertThrows(NullPointerException.class, () -> MapUtil.mapValues(map, null));
    }

    @Test
    void shouldConvertToCollectionsAndMaps() {
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put(2, null);

        assertEquals(2, MapUtil.toList(map).size());
        assertEquals(List.of("a", 2), MapUtil.toKeyList(map));
        assertEquals(java.util.Arrays.asList(1, null), MapUtil.toValueList(map));
        assertEquals(Set.copyOf(map.entrySet()), Set.copyOf(MapUtil.toSet(map)));
        assertEquals(1, MapUtil.toObjectMap(map).get("a"));
        assertTrue(MapUtil.toObjectMap(map).containsKey("2"));
        assertEquals("1", MapUtil.toStringMap(map).get("a"));
        assertInstanceOf(LinkedHashMap.class, MapUtil.toLinkedMap(map));
    }
}
