package io.github.atengk.enums.util;

import io.github.atengk.enums.sample.AnnotatedItemEnum;
import io.github.atengk.enums.sample.FieldOnlyEnum;
import io.github.atengk.enums.sample.UserStatusEnum;
import io.github.atengk.utils.enumutil.EnumUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnumUtilFieldExtractionTest {

    @Test
    void shouldExtractStandardFields() {
        assertEquals("ENABLED", EnumUtil.getName(UserStatusEnum.ENABLED));
        assertEquals(0, EnumUtil.getOrdinal(UserStatusEnum.ENABLED));
        assertEquals(1, EnumUtil.getCode(UserStatusEnum.ENABLED));
        assertEquals("启用", EnumUtil.getLabel(UserStatusEnum.ENABLED));
        assertEquals("用户可正常登录", EnumUtil.getDesc(UserStatusEnum.ENABLED));
        assertEquals(1, EnumUtil.getSort(UserStatusEnum.ENABLED));
        assertTrue(EnumUtil.getEnabled(UserStatusEnum.ENABLED));
    }

    @Test
    void shouldExtractReflectionFields() {
        assertEquals("L", EnumUtil.getCode(FieldOnlyEnum.LOW));
        assertEquals("低", EnumUtil.getLabel(FieldOnlyEnum.LOW));
        assertEquals(20, EnumUtil.getSort(FieldOnlyEnum.LOW));
        assertTrue(EnumUtil.getEnabled(FieldOnlyEnum.LOW));
        assertEquals("H", EnumUtil.getFieldValue(FieldOnlyEnum.HIGH, "code"));
        assertEquals("高", EnumUtil.getFieldValue(FieldOnlyEnum.HIGH, item -> EnumUtil.getLabel(item)));
    }

    @Test
    void shouldExtractAnnotationFields() {
        assertEquals("成功", EnumUtil.getLabel(AnnotatedItemEnum.SUCCESS));
        assertEquals("操作成功", EnumUtil.getDesc(AnnotatedItemEnum.SUCCESS));
        assertEquals(1, EnumUtil.getSort(AnnotatedItemEnum.SUCCESS));
        assertFalse(EnumUtil.getEnabled(AnnotatedItemEnum.FAIL));
        assertEquals("result", EnumUtil.getFieldValue(AnnotatedItemEnum.SUCCESS, "group"));
    }

    @Test
    void shouldReturnNullForNullEnumValue() {
        assertNull(EnumUtil.getName(null));
        assertNull(EnumUtil.getOrdinal(null));
        assertNull(EnumUtil.getCode(null));
        assertNull(EnumUtil.getLabel(null));
        assertNull(EnumUtil.getDesc(null));
        assertNull(EnumUtil.getSort(null));
        assertNull(EnumUtil.getEnabled(null));
    }
}
