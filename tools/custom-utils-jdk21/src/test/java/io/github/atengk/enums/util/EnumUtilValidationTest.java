package io.github.atengk.enums.util;

import io.github.atengk.enums.sample.UserStatusEnum;
import io.github.atengk.utils.enumutil.EnumUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnumUtilValidationTest {

    @Test
    void shouldValidateContainsMethods() {
        assertTrue(EnumUtil.containsName(UserStatusEnum.class, "ENABLED"));
        assertTrue(EnumUtil.containsCode(UserStatusEnum.class, 1));
        assertTrue(EnumUtil.containsLabel(UserStatusEnum.class, "禁用"));
        assertTrue(EnumUtil.containsOrdinal(UserStatusEnum.class, 0));
        assertTrue(EnumUtil.containsFieldValue(UserStatusEnum.class, "enabled", false));
        assertTrue(EnumUtil.isValidCode(UserStatusEnum.class, "1"));
        assertTrue(EnumUtil.isValidName(UserStatusEnum.class, "DISABLED"));
    }

    @Test
    void shouldValidateInvalidValues() {
        assertFalse(EnumUtil.containsName(UserStatusEnum.class, "enabled"));
        assertFalse(EnumUtil.containsCode(UserStatusEnum.class, 99));
        assertFalse(EnumUtil.containsLabel(UserStatusEnum.class, "未知"));
        assertFalse(EnumUtil.containsOrdinal(UserStatusEnum.class, -1));
        assertFalse(EnumUtil.isValidCode(UserStatusEnum.class, null));
    }

    @Test
    void shouldRequireValidValues() {
        assertDoesNotThrow(() -> EnumUtil.requireValidCode(UserStatusEnum.class, 1));
        assertDoesNotThrow(() -> EnumUtil.requireValidName(UserStatusEnum.class, "ENABLED"));
        assertEquals(UserStatusEnum.ENABLED, EnumUtil.requireByCode(UserStatusEnum.class, 1));
        assertEquals(UserStatusEnum.DISABLED, EnumUtil.requireByName(UserStatusEnum.class, "DISABLED"));
    }

    @Test
    void shouldThrowWhenRequiredValueInvalid() {
        assertThrows(IllegalArgumentException.class, () -> EnumUtil.requireByCode(UserStatusEnum.class, 100));
        assertThrows(IllegalArgumentException.class, () -> EnumUtil.requireByName(UserStatusEnum.class, "UNKNOWN"));
        assertThrows(IllegalArgumentException.class, () -> EnumUtil.requireValidCode(UserStatusEnum.class, null));
    }
}
