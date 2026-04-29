package io.github.atengk.maputil;

import io.github.atengk.utils.MapUtil;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BasicMapUtilTest {

    @Test
    void shouldCheckEmptyAndSizeSafely() {
        assertTrue(MapUtil.isEmpty(null));
        assertTrue(MapUtil.isEmpty(Map.of()));
        assertFalse(MapUtil.isNotEmpty(null));
        assertEquals(0, MapUtil.size(null));
        assertEquals(1, MapUtil.size(Map.of("a", 1)));
        assertTrue(MapUtil.isNullOrEmpty(Map.of()));
    }

    @Test
    void shouldCheckKeyAndValueSafely() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put(null, 2);

        assertTrue(MapUtil.containsKey(map, "a"));
        assertTrue(MapUtil.hasKey(map, null));
        assertTrue(MapUtil.containsValue(map, 1));
        assertTrue(MapUtil.hasValue(map, 2));
        assertFalse(MapUtil.containsKey(null, "a"));
        assertFalse(MapUtil.containsValue(null, 1));
    }
}
