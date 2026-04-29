package io.github.atengk.maputil;

import io.github.atengk.utils.MapUtil;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class CreationMapUtilTest {

    @Test
    void shouldCreateCommonMaps() {
        assertInstanceOf(HashMap.class, MapUtil.newHashMap());
        assertInstanceOf(LinkedHashMap.class, MapUtil.newLinkedHashMap());
        assertInstanceOf(ConcurrentHashMap.class, MapUtil.newConcurrentHashMap());
        assertThrows(IllegalArgumentException.class, () -> MapUtil.newHashMap(-1));
        assertThrows(IllegalArgumentException.class, () -> MapUtil.newLinkedHashMap(-1));
    }

    @Test
    void shouldCreateOfAndPairMaps() {
        Map<String, Integer> one = MapUtil.of("a", 1);
        assertEquals(1, one.get("a"));
        assertThrows(UnsupportedOperationException.class, () -> one.put("b", 2));

        Map<String, Integer> two = MapUtil.of("a", 1, "b", 2);
        assertEquals(2, two.size());

        Map<String, Integer> mutable = MapUtil.mutableOf("a", 1, "b", 2);
        mutable.put("c", 3);
        assertEquals(3, mutable.size());

        Map<String, Integer> linked = MapUtil.linkedOf("a", 1, "b", 2);
        assertEquals("a", linked.keySet().iterator().next());
        assertThrows(IllegalArgumentException.class, () -> MapUtil.mutableOf("a"));
    }

    @Test
    void shouldBuildMapWithBuilder() {
        Map<String, Integer> built = MapUtil.<String, Integer>builder()
                .put("a", 1)
                .putIfNotNull("b", 2)
                .putIfNotNull("c", null)
                .putAll(Map.of("d", 4))
                .build();

        assertEquals(Map.of("a", 1, "b", 2, "d", 4), built);
        assertThrows(UnsupportedOperationException.class, () -> built.put("e", 5));
        Map<String, Integer> mutableBuilt = MapUtil.<String, Integer>builder().put("a", 1).buildMutable();
        mutableBuilt.put("b", 2);
        assertEquals(2, mutableBuilt.size());
    }
}
