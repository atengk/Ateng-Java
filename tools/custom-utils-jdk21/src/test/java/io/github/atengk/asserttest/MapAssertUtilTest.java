package io.github.atengk.asserttest;

import io.github.atengk.utils.AssertUtil;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapAssertUtilTest {

    @Test
    void shouldPassWhenMapRulesAreValid() {
        Map<String, Integer> values = Map.of("a", 1, "b", 2);

        assertSame(values, AssertUtil.notEmpty(values, "Map 不能为空"));
        assertDoesNotThrow(() -> AssertUtil.isEmpty(Map.of(), "Map 必须为空"));
        assertSame(values, AssertUtil.containsKey(values, "a", "必须包含 key"));
        assertSame(values, AssertUtil.notContainsKey(values, "x", "不能包含 key"));
        assertSame(values, AssertUtil.containsValue(values, 1, "必须包含 value"));
        assertSame(values, AssertUtil.notContainsValue(values, 9, "不能包含 value"));
        assertSame(values, AssertUtil.requiredKeys(values, "必须包含全部 key", "a", "b"));
        assertSame(values, AssertUtil.noNullKeys(values, "不能包含 null key"));
        assertSame(values, AssertUtil.noNullValues(values, "不能包含 null value"));
    }

    @Test
    void shouldThrowWhenMapRulesAreInvalid() {
        Map<String, Integer> values = Map.of("a", 1, "b", 2);

        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notEmpty(Map.of(), "Map 不能为空"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.isEmpty(values, "Map 必须为空"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.containsKey(values, "x", "必须包含 key"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notContainsKey(values, "a", "不能包含 key"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.containsValue(values, 9, "必须包含 value"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notContainsValue(values, 1, "不能包含 value"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.requiredKeys(values, "必须包含全部 key", "a", "x"));
    }

    @Test
    void shouldThrowWhenMapHasNullKeyOrNullValue() {
        Map<String, Integer> nullKeyMap = new HashMap<>();
        nullKeyMap.put(null, 1);

        Map<String, Integer> nullValueMap = new HashMap<>();
        nullValueMap.put("a", null);

        assertThrows(IllegalArgumentException.class, () -> AssertUtil.noNullKeys(nullKeyMap, "不能包含 null key"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.noNullValues(nullValueMap, "不能包含 null value"));
    }
}
