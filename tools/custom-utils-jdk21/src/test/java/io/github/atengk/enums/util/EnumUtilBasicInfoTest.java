package io.github.atengk.enums.util;

import io.github.atengk.enums.sample.SimpleEnum;
import io.github.atengk.enums.sample.UserStatusEnum;
import io.github.atengk.utils.EnumUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnumUtilBasicInfoTest {

    @Test
    void shouldReturnBasicEnumInfo() {
        assertArrayEquals(UserStatusEnum.values(), EnumUtil.valuesOf(UserStatusEnum.class));
        assertEquals(List.of(UserStatusEnum.ENABLED, UserStatusEnum.DISABLED), EnumUtil.listOf(UserStatusEnum.class));
        assertEquals(List.of("ENABLED", "DISABLED"), EnumUtil.namesOf(UserStatusEnum.class));
        assertEquals(List.of(0, 1), EnumUtil.ordinalsOf(UserStatusEnum.class));
        assertEquals(2, EnumUtil.sizeOf(UserStatusEnum.class));
        assertTrue(EnumUtil.isEnum(UserStatusEnum.class));
        assertFalse(EnumUtil.isEnum(String.class));
        assertTrue(EnumUtil.isEnumValue(UserStatusEnum.class, "ENABLED"));
        assertFalse(EnumUtil.isEnumValue(UserStatusEnum.class, "UNKNOWN"));
    }

    @Test
    void shouldReturnEnumClassNames() {
        assertTrue(EnumUtil.getEnumClassName(UserStatusEnum.class).endsWith("UserStatusEnum"));
        assertEquals("UserStatusEnum", EnumUtil.getEnumSimpleName(UserStatusEnum.class));
    }

    @Test
    void shouldSupportSimpleEnumWithoutInterface() {
        assertEquals("FIRST", EnumUtil.getCode(SimpleEnum.FIRST));
        assertEquals("FIRST", EnumUtil.getLabel(SimpleEnum.FIRST));
    }

    @Test
    void shouldRejectInvalidEnumClass() {
        assertThrows(NullPointerException.class, () -> EnumUtil.valuesOf(null));
    }
}
