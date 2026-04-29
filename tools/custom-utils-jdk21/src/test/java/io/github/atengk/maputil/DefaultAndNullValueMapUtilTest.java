package io.github.atengk.maputil;

import io.github.atengk.utils.MapUtil;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultAndNullValueMapUtilTest {

    @Test
    void shouldHandleNullDefaultAndEmptyConversion() {
        Map<String, Integer> defaultMap = Map.of("d", 1);

        assertTrue(MapUtil.emptyIfNull(null).isEmpty());
        assertSame(defaultMap, MapUtil.defaultIfNull(null, defaultMap));
        assertNull(MapUtil.nullIfEmpty(Map.of()));
        assertNull(MapUtil.emptyToNull(null));
        assertEquals(defaultMap, MapUtil.nullIfEmpty(defaultMap));
    }

    @Test
    void shouldRemoveNullAndBlankValues() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(null, "null-key");
        map.put("a", null);
        map.put("b", " ");
        map.put("c", "  ok  ");
        map.put("d", 1);

        assertFalse(MapUtil.removeNullValue(map).containsKey("a"));
        assertFalse(MapUtil.removeNullKey(map).containsKey(null));
        assertFalse(MapUtil.removeBlankValue(map).containsKey("b"));
        assertEquals("ok", MapUtil.trimStringValue(map).get("c"));
        assertEquals(1, MapUtil.trimStringValue(map).get("d"));
    }
}
