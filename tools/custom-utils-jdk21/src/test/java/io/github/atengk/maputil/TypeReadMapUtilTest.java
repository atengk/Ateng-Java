package io.github.atengk.maputil;

import io.github.atengk.utils.MapUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TypeReadMapUtilTest {

    enum Status {
        ENABLED,
        DISABLED
    }

    @Test
    void shouldReadCommonTypes() {
        Map<String, Object> map = Map.of(
                "str", 123,
                "int", "12",
                "long", 13,
                "double", "1.5",
                "decimal", "9.99",
                "bool", "yes",
                "date", "2026-04-29",
                "time", "2026-04-29 10:30:00",
                "enum", "ENABLED"
        );

        assertEquals("123", MapUtil.getStr(map, "str"));
        assertEquals("default", MapUtil.getStr(map, "missing", "default"));
        assertEquals(12, MapUtil.getInt(map, "int"));
        assertEquals(13L, MapUtil.getLong(map, "long"));
        assertEquals(1.5D, MapUtil.getDouble(map, "double"));
        assertEquals(new BigDecimal("9.99"), MapUtil.getBigDecimal(map, "decimal"));
        assertTrue(MapUtil.getBool(map, "bool"));
        assertEquals(LocalDate.of(2026, 4, 29), MapUtil.getLocalDate(map, "date"));
        assertEquals(LocalDateTime.of(2026, 4, 29, 10, 30), MapUtil.getLocalDateTime(map, "time"));
        assertNotNull(MapUtil.getDate(map, "date"));
        assertEquals(Status.ENABLED, MapUtil.getEnum(map, "enum", Status.class));
    }

    @Test
    void shouldReadListAndMap() {
        Map<String, Object> map = Map.of(
                "list", List.of("1", "2"),
                "array", new String[]{"3", "4"},
                "nested", Map.of("a", 1)
        );

        assertEquals(List.of(1, 2), MapUtil.getList(map, "list", Integer.class));
        assertEquals(List.of(3L, 4L), MapUtil.getList(map, "array", Long.class));
        assertEquals(1, MapUtil.getMap(map, "nested").get("a"));
        assertTrue(MapUtil.getList(map, "missing", String.class).isEmpty());
    }

    @Test
    void shouldThrowOnInvalidTypeConversion() {
        Map<String, Object> map = Map.of("bad", "abc", "notMap", "x");

        assertThrows(IllegalArgumentException.class, () -> MapUtil.getInt(map, "bad"));
        assertThrows(IllegalArgumentException.class, () -> MapUtil.getBool(map, "bad"));
        assertThrows(IllegalArgumentException.class, () -> MapUtil.getMap(map, "notMap"));
        assertThrows(IllegalArgumentException.class, () -> MapUtil.getEnum(map, "bad", Status.class));
        assertThrows(NullPointerException.class, () -> MapUtil.getList(map, "bad", null));
    }
}
