package io.github.atengk.enums.util;

import io.github.atengk.enums.sample.OrderStatusEnum;
import io.github.atengk.enums.sample.UserStatusEnum;
import io.github.atengk.model.EnumOption;
import io.github.atengk.utils.EnumUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnumUtilFrontendDictTest {

    @Test
    void shouldBuildOptions() {
        List<EnumOption> options = EnumUtil.toOptions(UserStatusEnum.class);
        assertEquals(2, options.size());
        assertEquals(1, options.getFirst().code());
        assertEquals("启用", options.getFirst().label());
        assertEquals("ENABLED", options.getFirst().name());
    }

    @Test
    void shouldBuildEnabledAndCustomFieldOptions() {
        List<EnumOption> enabledOptions = EnumUtil.toOptions(UserStatusEnum.class, true);
        assertEquals(1, enabledOptions.size());
        assertEquals(1, enabledOptions.getFirst().code());
        assertEquals("启用", EnumUtil.toOptions(UserStatusEnum.class, "code", "label").getFirst().label());
    }

    @Test
    void shouldBuildDictObjects() {
        assertEquals("user-status", EnumUtil.toDict(UserStatusEnum.class).get("key"));
        assertEquals(2, EnumUtil.toDictList(UserStatusEnum.class).size());
        assertTrue(EnumUtil.toDictMap(UserStatusEnum.class).containsKey("user-status"));
        assertTrue(EnumUtil.toDictMap(List.of(UserStatusEnum.class, OrderStatusEnum.class)).containsKey("order-status"));
        assertEquals("启用", EnumUtil.toOptionMap(UserStatusEnum.class).get(1).label());
    }

    @Test
    void shouldBuildSimpleFullGroupedTreeOptions() {
        assertNull(EnumUtil.toSimpleOptions(UserStatusEnum.class).getFirst().name());
        assertFalse(EnumUtil.toFullOptions(UserStatusEnum.class).getFirst().extra().isEmpty());
        Map<Object, List<EnumOption>> grouped = EnumUtil.toGroupedOptions(OrderStatusEnum.class);
        assertEquals(2, grouped.get("open").size());
        assertFalse(EnumUtil.toTreeOptions(OrderStatusEnum.class).isEmpty());
        assertEquals("启用", EnumUtil.getOptionByCode(UserStatusEnum.class, 1).label());
        assertNull(EnumUtil.getOptionByCode(UserStatusEnum.class, 99));
    }
}
