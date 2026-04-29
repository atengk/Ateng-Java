package io.github.atengk.maputil;

import io.github.atengk.utils.MapUtil;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WriteUpdateMapUtilTest {

    @Test
    void shouldWriteConditionally() {
        Map<String, Object> map = new LinkedHashMap<>();

        MapUtil.putIfNotNull(map, "a", 1);
        MapUtil.putIfNotNull(map, "b", null);
        assertEquals(Map.of("a", 1), map);

        Map<String, String> strMap = new LinkedHashMap<>();
        MapUtil.putIfNotBlank(strMap, "a", " ");
        MapUtil.putIfNotBlank(strMap, "b", "ok");
        assertEquals(Map.of("b", "ok"), strMap);

        assertThrows(NullPointerException.class, () -> MapUtil.putIfNotNull(null, "a", 1));
    }

    @Test
    void shouldUpdateAndRemove() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", null);

        MapUtil.putIfAbsent(map, "a", 9);
        MapUtil.putIfAbsent(map, "c", 3);
        MapUtil.putIfPresent(map, "a", 10);
        MapUtil.setDefault(map, "b", 2);
        MapUtil.replaceValue(map, "c", 30);
        MapUtil.removeKeys(map, List.of("a"));

        assertEquals(Map.of("b", 2, "c", 30), map);
    }

    @Test
    void shouldPutAllWithNullPolicies() {
        Map<String, Integer> target = new LinkedHashMap<>();
        MapUtil.putAllIfNotNull(target, Map.of("a", 1));
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("b", null);
        source.put("c", 3);
        MapUtil.putAllIgnoreNullValue(target, source);

        assertEquals(Map.of("a", 1, "c", 3), target);
    }
}
