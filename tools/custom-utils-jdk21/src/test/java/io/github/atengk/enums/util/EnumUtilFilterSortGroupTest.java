package io.github.atengk.enums.util;

import io.github.atengk.enums.sample.OrderStatusEnum;
import io.github.atengk.enums.sample.UserStatusEnum;
import io.github.atengk.utils.EnumUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnumUtilFilterSortGroupTest {

    @Test
    void shouldFilterEnums() {
        assertEquals(List.of(UserStatusEnum.ENABLED), EnumUtil.filterEnabled(UserStatusEnum.class));
        assertEquals(List.of(UserStatusEnum.DISABLED), EnumUtil.filterDisabled(UserStatusEnum.class));
        assertEquals(List.of(OrderStatusEnum.CREATED, OrderStatusEnum.PAID), EnumUtil.filterByGroup(OrderStatusEnum.class, "open"));
        assertEquals(List.of(UserStatusEnum.DISABLED), EnumUtil.filterByField(UserStatusEnum.class, "enabled", false));
    }

    @Test
    void shouldSortEnums() {
        assertEquals(List.of(UserStatusEnum.ENABLED, UserStatusEnum.DISABLED), EnumUtil.sortByOrdinal(UserStatusEnum.class));
        assertEquals(List.of(UserStatusEnum.DISABLED, UserStatusEnum.ENABLED), EnumUtil.sortByCode(UserStatusEnum.class));
        assertEquals(List.of(UserStatusEnum.ENABLED, UserStatusEnum.DISABLED), EnumUtil.sortBySort(UserStatusEnum.class));
        assertEquals(List.of(UserStatusEnum.DISABLED, UserStatusEnum.ENABLED), EnumUtil.sortByField(UserStatusEnum.class, "label"));
    }

    @Test
    void shouldGroupEnums() {
        Map<Object, List<OrderStatusEnum>> groupMap = EnumUtil.groupByGroup(OrderStatusEnum.class);
        assertEquals(2, groupMap.get("open").size());
        Map<String, List<Class<?>>> moduleMap = EnumUtil.groupByModule(List.of(UserStatusEnum.class, OrderStatusEnum.class));
        assertEquals(List.of(UserStatusEnum.class), moduleMap.get("system"));
        assertEquals(List.of(OrderStatusEnum.class), moduleMap.get("order"));
    }

    @Test
    void shouldHandleEmptyGroupByModule() {
        assertTrue(EnumUtil.groupByModule(null).isEmpty());
        assertTrue(EnumUtil.groupByModule(List.of(String.class)).isEmpty());
    }
}
