package io.github.atengk.enums.util;

import io.github.atengk.enums.sample.UserStatusEnum;
import io.github.atengk.utils.EnumUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnumUtilCollectionMapTest {

    @Test
    void shouldBuildLists() {
        assertEquals(List.of("ENABLED", "DISABLED"), EnumUtil.toNameList(UserStatusEnum.class));
        assertEquals(List.of(1, 0), EnumUtil.toCodeList(UserStatusEnum.class));
        assertEquals(List.of("启用", "禁用"), EnumUtil.toLabelList(UserStatusEnum.class));
    }

    @Test
    void shouldBuildMaps() {
        assertEquals(UserStatusEnum.ENABLED, EnumUtil.toMapByName(UserStatusEnum.class).get("ENABLED"));
        assertEquals(UserStatusEnum.DISABLED, EnumUtil.toMapByCode(UserStatusEnum.class).get(0));
        assertEquals(UserStatusEnum.ENABLED, EnumUtil.toMapByLabel(UserStatusEnum.class).get("启用"));
        assertEquals("禁用", EnumUtil.toFieldMap(UserStatusEnum.class, "code", "label").get(0));
        assertEquals("启用", EnumUtil.toValueMap(UserStatusEnum.class, UserStatusEnum::getCode, UserStatusEnum::getLabel).get(1));
    }

    @Test
    void shouldGroupEnums() {
        Map<String, List<UserStatusEnum>> byCustomGroup = EnumUtil.groupBy(UserStatusEnum.class, UserStatusEnum::getGroup);
        assertEquals(List.of(UserStatusEnum.ENABLED, UserStatusEnum.DISABLED), byCustomGroup.get("normal"));
        Map<Object, List<UserStatusEnum>> byField = EnumUtil.groupByField(UserStatusEnum.class, "enabled");
        assertEquals(List.of(UserStatusEnum.ENABLED), byField.get(true));
    }

    @Test
    void shouldRejectInvalidMapArgs() {
        assertThrows(IllegalArgumentException.class, () -> EnumUtil.toFieldMap(UserStatusEnum.class, "", "label"));
        assertThrows(NullPointerException.class, () -> EnumUtil.toEnumMap(UserStatusEnum.class, null));
    }
}
