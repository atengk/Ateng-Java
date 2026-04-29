package io.github.atengk.enums.util;

import io.github.atengk.enums.sample.FieldOnlyEnum;
import io.github.atengk.enums.sample.UserStatusEnum;
import io.github.atengk.utils.EnumUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnumUtilQueryMatchingTest {

    @Test
    void shouldGetEnumByCommonKeys() {
        assertEquals(UserStatusEnum.ENABLED, EnumUtil.getByName(UserStatusEnum.class, "ENABLED"));
        assertEquals(UserStatusEnum.ENABLED, EnumUtil.getByNameIgnoreCase(UserStatusEnum.class, "enabled"));
        assertEquals(UserStatusEnum.DISABLED, EnumUtil.getByOrdinal(UserStatusEnum.class, 1));
        assertEquals(UserStatusEnum.ENABLED, EnumUtil.getByCode(UserStatusEnum.class, 1));
        assertEquals(UserStatusEnum.DISABLED, EnumUtil.getByCode(UserStatusEnum.class, "0"));
        assertEquals(UserStatusEnum.ENABLED, EnumUtil.getByLabel(UserStatusEnum.class, "启用"));
        assertEquals(UserStatusEnum.ENABLED, EnumUtil.getByDesc(UserStatusEnum.class, "用户可正常登录"));
    }

    @Test
    void shouldGetEnumByFieldAndPredicate() {
        assertEquals(FieldOnlyEnum.HIGH, EnumUtil.getFirstByField(FieldOnlyEnum.class, "code", "H"));
        assertEquals(List.of(FieldOnlyEnum.HIGH), EnumUtil.getListByField(FieldOnlyEnum.class, "enabled", false));
        assertEquals(UserStatusEnum.DISABLED, EnumUtil.getByPredicate(UserStatusEnum.class, item -> !item.getEnabled()));
        assertEquals(List.of(UserStatusEnum.ENABLED), EnumUtil.getListByPredicate(UserStatusEnum.class, UserStatusEnum::getEnabled));
    }

    @Test
    void shouldReturnNullWhenNotMatched() {
        assertNull(EnumUtil.getByName(UserStatusEnum.class, "UNKNOWN"));
        assertNull(EnumUtil.getByNameIgnoreCase(UserStatusEnum.class, null));
        assertNull(EnumUtil.getByOrdinal(UserStatusEnum.class, -1));
        assertNull(EnumUtil.getByOrdinal(UserStatusEnum.class, 99));
        assertNull(EnumUtil.getByCode(UserStatusEnum.class, null));
        assertNull(EnumUtil.getByLabel(UserStatusEnum.class, ""));
        assertNull(EnumUtil.getByDesc(UserStatusEnum.class, "不存在"));
    }

    @Test
    void shouldRejectInvalidFieldName() {
        assertThrows(IllegalArgumentException.class, () -> EnumUtil.getFirstByField(UserStatusEnum.class, " ", 1));
        assertThrows(NullPointerException.class, () -> EnumUtil.getByPredicate(UserStatusEnum.class, null));
    }
}
