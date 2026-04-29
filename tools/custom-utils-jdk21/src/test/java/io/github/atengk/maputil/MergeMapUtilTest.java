package io.github.atengk.maputil;

import io.github.atengk.utils.MapUtil;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MergeMapUtilTest {

    @Test
    void shouldMergeWithDifferentPolicies() {
        Map<String, Integer> base = new LinkedHashMap<>();
        base.put("a", 1);
        base.put("b", 2);
        Map<String, Integer> override = new LinkedHashMap<>();
        override.put("b", 20);
        override.put("c", null);

        assertEquals(20, MapUtil.merge(base, override).get("b"));
        assertEquals(20, MapUtil.mergeOverwrite(base, override).get("b"));
        assertEquals(20, MapUtil.mergeIgnoreNull(base, override).get("b"));
        assertFalse(MapUtil.mergeIgnoreNull(base, override).containsKey("c"));
        assertEquals(2, MapUtil.mergeKeepOriginal(base, override).get("b"));
    }

    @Test
    void shouldMergeWithCustomFunctionAndList() {
        Map<String, Integer> left = Map.of("a", 1);
        Map<String, Integer> right = Map.of("a", 2, "b", 3);

        Map<String, Integer> merged = MapUtil.mergeWith(left, right, Integer::sum);
        assertEquals(Map.of("a", 3, "b", 3), merged);
        assertEquals(Map.of("a", 2), MapUtil.mergeList(List.of(Map.of("a", 1), Map.of("a", 2))));
        assertThrows(NullPointerException.class, () -> MapUtil.mergeWith(left, right, null));
    }

    @Test
    void shouldDeepMergeNestedMap() {
        Map<String, Object> base = new LinkedHashMap<>();
        base.put("user", Map.of("name", "Ateng", "age", 18));
        base.put("enabled", true);
        Map<String, Object> override = new LinkedHashMap<>();
        override.put("user", Map.of("age", 20));

        Map<String, Object> merged = MapUtil.deepMerge(base, override);
        assertEquals("Ateng", MapUtil.getByPath(merged, "user.name"));
        assertEquals(20, MapUtil.getByPath(merged, "user.age"));
        assertEquals(true, merged.get("enabled"));
    }
}
