package io.github.atengk.enums.util;

import io.github.atengk.enums.sample.UserStatusEnum;
import io.github.atengk.utils.enumutil.EnumUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EnumUtilDefaultExceptionCacheTest {

    @AfterEach
    void clean() {
        EnumUtil.enableCache();
        EnumUtil.clearCache();
    }

    @Test
    void shouldReturnDefaultValues() {
        assertEquals(UserStatusEnum.ENABLED, EnumUtil.getByCodeOrDefault(UserStatusEnum.class, 99, UserStatusEnum.ENABLED));
        assertEquals("默认", EnumUtil.getLabelOrDefault(UserStatusEnum.class, 99, "默认"));
        assertEquals("默认描述", EnumUtil.getDescOrDefault(UserStatusEnum.class, 99, "默认描述"));
        assertEquals(UserStatusEnum.ENABLED, EnumUtil.requireByCode(UserStatusEnum.class, 1, "不存在"));
        assertEquals(UserStatusEnum.ENABLED, EnumUtil.requireByName(UserStatusEnum.class, "ENABLED", "不存在"));
        assertTrue(EnumUtil.optionalByCode(UserStatusEnum.class, 1).isPresent());
        assertTrue(EnumUtil.optionalByName(UserStatusEnum.class, "UNKNOWN").isEmpty());
    }

    @Test
    void shouldThrowCustomMessages() {
        IllegalArgumentException codeException = assertThrows(IllegalArgumentException.class, () -> EnumUtil.requireByCode(UserStatusEnum.class, 99, "编码错误"));
        assertEquals("编码错误", codeException.getMessage());
        IllegalArgumentException nameException = assertThrows(IllegalArgumentException.class, () -> EnumUtil.requireByName(UserStatusEnum.class, "UNKNOWN", "名称错误"));
        assertEquals("名称错误", nameException.getMessage());
    }

    @Test
    void shouldManageCache() {
        EnumUtil.enableCache();
        assertTrue(EnumUtil.isCacheEnabled());
        assertEquals(0, EnumUtil.getCacheSize());
        EnumUtil.getFieldValue(UserStatusEnum.ENABLED, "code");
        assertTrue(EnumUtil.getCacheSize() >= 1);
        Set<Class<?>> classes = EnumUtil.getCachedEnumClasses();
        assertTrue(classes.contains(UserStatusEnum.class));
        EnumUtil.refreshCache(UserStatusEnum.class);
        assertTrue(EnumUtil.getCachedEnumClasses().contains(UserStatusEnum.class));
        EnumUtil.refreshAllCache();
        assertTrue(EnumUtil.getCacheSize() >= 1);
        EnumUtil.clearCache(UserStatusEnum.class);
        assertFalse(EnumUtil.getCachedEnumClasses().contains(UserStatusEnum.class));
        EnumUtil.clearCache();
        assertEquals(0, EnumUtil.getCacheSize());
    }

    @Test
    void shouldDisableCache() {
        EnumUtil.disableCache();
        assertFalse(EnumUtil.isCacheEnabled());
        EnumUtil.getFieldValue(UserStatusEnum.ENABLED, "code");
        assertEquals(0, EnumUtil.getCacheSize());
    }
}
