package io.github.atengk.maputil;

import io.github.atengk.utils.MapUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateMapUtilTest {

    @Test
    void shouldRequireMapAndKeys() {
        Map<String, Object> map = Map.of("a", 1, "b", "ok");

        assertSame(map, MapUtil.requireNotEmpty(map, "empty"));
        assertDoesNotThrow(() -> MapUtil.requireKey(map, "a", "missing"));
        assertDoesNotThrow(() -> MapUtil.requireKeys(map, List.of("a", "b"), "missing"));
        assertDoesNotThrow(() -> MapUtil.requireValue(map, "a", "missing"));
    }

    @Test
    void shouldDetectMissingKeys() {
        Map<String, Object> map = Map.of("a", 1);

        assertEquals(List.of("b", "c"), MapUtil.missingKeys(map, List.of("a", "b", "c")));
        assertTrue(MapUtil.hasAllKeys(map, List.of("a")));
        assertFalse(MapUtil.hasAllKeys(map, List.of("a", "b")));
        assertTrue(MapUtil.hasAnyKey(map, List.of("x", "a")));
        assertFalse(MapUtil.hasAnyKey(null, List.of("a")));
    }

    @Test
    void shouldThrowOnInvalidRequiredState() {
        assertThrows(IllegalArgumentException.class, () -> MapUtil.requireNotEmpty(Map.of(), "empty"));
        assertThrows(IllegalArgumentException.class, () -> MapUtil.requireKey(Map.of(), "a", "missing"));
        assertThrows(IllegalArgumentException.class, () -> MapUtil.requireKeys(Map.of("a", 1), List.of("a", "b"), "missing"));
        assertThrows(IllegalArgumentException.class, () -> MapUtil.requireValue(Map.of("a", 1), "b", "missing"));
    }
}
