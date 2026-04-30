package io.github.atengk.enums.util;

import io.github.atengk.enums.sample.AnnotatedItemEnum;
import io.github.atengk.enums.sample.SimpleEnum;
import io.github.atengk.enums.sample.UserStatusEnum;
import io.github.atengk.utils.enumutil.model.EnumMetadata;
import io.github.atengk.utils.enumutil.EnumUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnumUtilMetadataTest {

    @Test
    void shouldGetEnumMetadata() {
        assertEquals("user-status", EnumUtil.getEnumKey(UserStatusEnum.class));
        assertEquals("用户状态", EnumUtil.getEnumTitle(UserStatusEnum.class));
        assertEquals("system", EnumUtil.getEnumModule(UserStatusEnum.class));
        assertEquals("用户账号状态字典", EnumUtil.getEnumDescription(UserStatusEnum.class));
        assertTrue(EnumUtil.isExposed(UserStatusEnum.class));
        assertFalse(EnumUtil.isDeprecated(UserStatusEnum.class));
        EnumMetadata metadata = EnumUtil.getEnumMetadata(UserStatusEnum.class);
        assertEquals("UserStatusEnum", metadata.simpleName());
        assertTrue(metadata.extraFields().contains("color"));
    }

    @Test
    void shouldFallbackEnumKey() {
        assertEquals("simple", EnumUtil.getEnumKey(SimpleEnum.class));
        assertEquals("SimpleEnum", EnumUtil.getEnumTitle(SimpleEnum.class));
    }

    @Test
    void shouldGetItemMetadata() {
        Map<String, Object> metadata = EnumUtil.getItemMetadata(AnnotatedItemEnum.SUCCESS);
        assertEquals("SUCCESS", metadata.get("name"));
        assertEquals("成功", metadata.get("label"));
        assertEquals("操作成功", metadata.get("desc"));
        assertEquals("root", metadata.get("parentCode"));
    }

    @Test
    void shouldRejectInvalidMetadataClass() {
        assertThrows(IllegalArgumentException.class, () -> EnumUtil.getEnumKey(String.class));
    }
}
