package io.github.atengk.maputil;

import io.github.atengk.utils.MapUtil;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class SafeReadMapUtilTest {

    @Test
    void shouldReadValuesSafely() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a", "1");
        map.put("b", null);

        assertEquals("1", MapUtil.get(map, "a"));
        assertNull(MapUtil.getOrNull(map, "x"));
        assertEquals("d", MapUtil.getOrDefault(map, "x", "d"));
        assertEquals("d", MapUtil.getOrDefault(map, "b", "d"));
        assertEquals("1", MapUtil.getRequired(map, "a"));
        assertThrows(NoSuchElementException.class, () -> MapUtil.getRequired(map, "b"));
        assertThrows(NoSuchElementException.class, () -> MapUtil.getRequired(map, "x"));
    }

    @Test
    void shouldReadFirstValues() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a", null);
        map.put("b", "2");

        assertNull(MapUtil.getFirst(map, List.of("a", "b")));
        assertEquals("2", MapUtil.getFirstNonNull(map, List.of("a", "b")));
        assertNull(MapUtil.getFirst(null, List.of("a")));
        assertNull(MapUtil.getFirstNonNull(map, null));
    }

    @Test
    void shouldReadNestedPath() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("user", Map.of("name", "Ateng"));

        assertEquals("Ateng", MapUtil.getByPath(map, "user.name"));
        assertEquals("N/A", MapUtil.getByPath(map, "user.age", "N/A"));
        assertEquals("N/A", MapUtil.getByPath(null, "user.age", "N/A"));
        assertThrows(IllegalArgumentException.class, () -> MapUtil.getByPath(map, "user..name"));
    }
}
