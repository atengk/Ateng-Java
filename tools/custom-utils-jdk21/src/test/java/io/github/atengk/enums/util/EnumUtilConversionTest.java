package io.github.atengk.enums.util;

import io.github.atengk.enums.sample.UserStatusEnum;
import io.github.atengk.utils.enumutil.EnumUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnumUtilConversionTest {

    @Test
    void shouldConvertBetweenEnumAndFields() {
        assertEquals(UserStatusEnum.ENABLED, EnumUtil.codeToEnum(UserStatusEnum.class, 1));
        assertEquals(UserStatusEnum.DISABLED, EnumUtil.nameToEnum(UserStatusEnum.class, "DISABLED"));
        assertEquals(UserStatusEnum.DISABLED, EnumUtil.ordinalToEnum(UserStatusEnum.class, 1));
        assertEquals(1, EnumUtil.enumToCode(UserStatusEnum.ENABLED));
        assertEquals("启用", EnumUtil.enumToLabel(UserStatusEnum.ENABLED));
        assertEquals("用户可正常登录", EnumUtil.enumToDesc(UserStatusEnum.ENABLED));
    }

    @Test
    void shouldConvertCodeNameAndLabel() {
        assertEquals("启用", EnumUtil.codeToLabel(UserStatusEnum.class, 1));
        assertEquals("用户禁止登录", EnumUtil.codeToDesc(UserStatusEnum.class, 0));
        assertEquals(1, EnumUtil.nameToCode(UserStatusEnum.class, "ENABLED"));
        assertEquals(0, EnumUtil.labelToCode(UserStatusEnum.class, "禁用"));
        assertEquals("禁用", EnumUtil.convert(UserStatusEnum.class, 0, "code", "label"));
    }

    @Test
    void shouldReturnNullWhenConvertFailed() {
        assertNull(EnumUtil.codeToEnum(UserStatusEnum.class, 100));
        assertNull(EnumUtil.nameToEnum(UserStatusEnum.class, "UNKNOWN"));
        assertNull(EnumUtil.ordinalToEnum(UserStatusEnum.class, 100));
        assertNull(EnumUtil.codeToLabel(UserStatusEnum.class, 100));
        assertNull(EnumUtil.convert(UserStatusEnum.class, 100, "code", "label"));
    }

    @Test
    void shouldRejectBlankConvertField() {
        assertThrows(IllegalArgumentException.class, () -> EnumUtil.convert(UserStatusEnum.class, 1, "", "label"));
        assertThrows(IllegalArgumentException.class, () -> EnumUtil.convert(UserStatusEnum.class, 1, "code", ""));
    }
}
