package io.github.atengk.maputil;

import io.github.atengk.utils.MapUtil;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NestedPathMapUtilTest {

    @Test
    void shouldPutContainsAndRemovePath() {
        Map<String, Object> map = new LinkedHashMap<>();

        MapUtil.putByPath(map, "user.profile.name", "Ateng");
        assertEquals("Ateng", MapUtil.getByPath(map, "user.profile.name"));
        assertTrue(MapUtil.containsPath(map, "user.profile.name"));
        assertFalse(MapUtil.containsPath(map, "user.profile.age"));
        assertEquals("Ateng", MapUtil.removeByPath(map, "user.profile.name"));
        assertFalse(MapUtil.containsPath(map, "user.profile.name"));
    }

    @Test
    void shouldFlattenAndUnflatten() {
        Map<String, Object> map = new LinkedHashMap<>();
        MapUtil.putByPath(map, "user.name", "Ateng");
        MapUtil.putByPath(map, "user.age", 18);
        map.put("enabled", true);

        Map<String, Object> flat = MapUtil.flatten(map);
        assertEquals("Ateng", flat.get("user.name"));
        assertEquals(18, flat.get("user.age"));
        assertEquals(true, flat.get("enabled"));

        Map<String, Object> restored = MapUtil.unflatten(flat);
        assertEquals("Ateng", MapUtil.getByPath(restored, "user.name"));
        assertEquals(18, MapUtil.getByPath(restored, "user.age"));
    }

    @Test
    void shouldRejectInvalidPath() {
        assertThrows(NullPointerException.class, () -> MapUtil.putByPath(null, "a.b", 1));
        assertThrows(IllegalArgumentException.class, () -> MapUtil.putByPath(new LinkedHashMap<>(), "a..b", 1));
        assertNull(MapUtil.removeByPath(null, "a"));
        assertFalse(MapUtil.containsPath(null, "a"));
    }
}
