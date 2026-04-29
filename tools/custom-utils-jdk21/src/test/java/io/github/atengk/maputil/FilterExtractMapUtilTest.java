package io.github.atengk.maputil;

import io.github.atengk.utils.MapUtil;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FilterExtractMapUtilTest {

    @Test
    void shouldFilterEntriesKeysAndValues() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", null);

        assertEquals(Map.of("b", 2), MapUtil.filter(map, (key, value) -> value != null && value > 1));
        assertEquals(Map.of("a", 1), MapUtil.filterKeys(map, key -> key.equals("a")));
        assertEquals(Map.of("b", 2), MapUtil.filterValues(map, value -> value != null && value == 2));
        assertThrows(NullPointerException.class, () -> MapUtil.filter(map, null));
    }

    @Test
    void shouldIncludeExcludePickAndOmit() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        assertEquals(Map.of("a", 1, "c", 3), MapUtil.includeKeys(map, List.of("a", "c")));
        assertEquals(Map.of("a", 1, "c", 3), MapUtil.pick(map, List.of("a", "c")));
        assertEquals(Map.of("a", 1), MapUtil.excludeKeys(map, List.of("b", "c")));
        assertEquals(Map.of("a", 1), MapUtil.omit(map, List.of("b", "c")));
    }

    @Test
    void shouldFilterNotNullAndNotBlank() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", null);
        assertEquals(Map.of("a", 1), MapUtil.filterNotNullValue(map));

        Map<String, String> strMap = new LinkedHashMap<>();
        strMap.put("a", " ");
        strMap.put("b", "ok");
        assertEquals(Map.of("b", "ok"), MapUtil.filterNotBlankValue(strMap));
    }
}
