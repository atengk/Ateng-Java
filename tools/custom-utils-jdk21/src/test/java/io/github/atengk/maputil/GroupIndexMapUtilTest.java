package io.github.atengk.maputil;

import io.github.atengk.utils.MapUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GroupIndexMapUtilTest {

    record Item(String code, String type, BigDecimal amount) {
    }

    @Test
    void shouldIndexAndGroupCollection() {
        List<Item> items = List.of(
                new Item("a", "x", BigDecimal.ONE),
                new Item("b", "x", BigDecimal.TEN),
                new Item("a", "y", BigDecimal.valueOf(2))
        );

        assertEquals("y", MapUtil.indexBy(items, Item::code).get("a").type());
        assertEquals(BigDecimal.valueOf(3), MapUtil.indexBy(items, Item::code, (oldItem, newItem) -> new Item(oldItem.code(), oldItem.type(), oldItem.amount().add(newItem.amount()))).get("a").amount());
        assertEquals(2, MapUtil.groupBy(items, Item::type).get("x").size());
        assertEquals(2L, MapUtil.groupCount(items, Item::type).get("x"));
        assertEquals(BigDecimal.valueOf(11), MapUtil.groupSum(items, Item::type, Item::amount).get("x"));
    }

    @Test
    void shouldConvertCollectionToMap() {
        List<Item> items = List.of(new Item("a", "x", BigDecimal.ONE), new Item("b", "y", BigDecimal.TEN));

        assertEquals(Map.of("a", "x", "b", "y"), MapUtil.toMap(items, Item::code, Item::type));
        assertEquals(List.of("a", "b"), MapUtil.toLinkedMap(items, Item::code, Item::type).keySet().stream().toList());
        assertThrows(NullPointerException.class, () -> MapUtil.groupBy(items, null));
    }
}
