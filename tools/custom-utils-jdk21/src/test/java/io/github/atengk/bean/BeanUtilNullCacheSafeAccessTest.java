package io.github.atengk.bean;

import io.github.atengk.utils.BeanUtil;
import io.github.atengk.bean.fixture.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BeanUtil 空值默认值、反射缓存和安全访问单元测试。
 *
 * @author Ateng
 * @since 2026-04-29
 */
@DisplayName("BeanUtil 空值默认值、反射缓存和安全访问")
class BeanUtilNullCacheSafeAccessTest {

    @Test
    @DisplayName("判断 null、空值和空白值")
    void shouldCheckNullEmptyAndBlankValues() {
        assertTrue(BeanUtil.isNull(null));
        assertTrue(BeanUtil.isNotNull("value"));
        assertTrue(BeanUtil.isBlank("   "));
        assertTrue(BeanUtil.isNotBlank("ateng"));
        assertTrue(BeanUtil.isEmpty(List.of()));
        assertTrue(BeanUtil.isNotEmpty(List.of("item")));
        assertTrue(BeanUtil.isBlankValue("   "));
    }

    @Test
    @DisplayName("返回默认值")
    void shouldReturnDefaultValues() {
        assertEquals("default", BeanUtil.defaultIfNull(null, "default"));
        assertEquals("supplier", BeanUtil.defaultIfNull(null, () -> "supplier"));
        assertEquals("default", BeanUtil.defaultIfEmpty("", "default"));
        assertEquals("supplier", BeanUtil.defaultIfEmpty(List.of(), () -> "supplier"));
        assertEquals("default", BeanUtil.defaultIfBlank("   ", "default"));
        assertEquals("a", BeanUtil.firstNonNull(null, "a", "b"));
        assertEquals("b", BeanUtil.firstNonEmpty("", "b"));
        assertEquals("", BeanUtil.nullToEmpty(null));
        assertNull(BeanUtil.emptyToNull(""));
        assertNull(BeanUtil.blankToNull("   "));
    }

    @Test
    @DisplayName("获取基本类型默认值")
    void shouldGetPrimitiveDefaultValues() {
        assertEquals(0, BeanUtil.getDefaultValue(int.class));
        assertEquals(false, BeanUtil.getDefaultValue(boolean.class));
        assertNull(BeanUtil.getDefaultValue(String.class));
    }

    @Test
    @DisplayName("获取空属性名列表")
    void shouldGetNullAndEmptyPropertyNames() {
        UserEntity user = new UserEntity(1001L, "");
        user.setAge(null);

        assertTrue(BeanUtil.getNullPropertyNames(user).contains("age"));
        assertTrue(BeanUtil.getNotNullPropertyNames(user).contains("id"));
        assertTrue(BeanUtil.getEmptyPropertyNames(user).contains("username"));
        assertTrue(BeanUtil.getBlankPropertyNames(user, List.of("username")).contains("username"));
    }

    @Test
    @DisplayName("空值时设置默认属性")
    void shouldSetDefaultPropertyValues() {
        UserEntity user = new UserEntity();

        assertTrue(BeanUtil.setDefaultIfNull(user, "username", "default-user"));
        assertFalse(BeanUtil.setDefaultIfNull(user, "username", "new-user"));
        assertEquals("default-user", user.getUsername());

        user.setUsername("");
        assertTrue(BeanUtil.setDefaultIfEmpty(user, "username", "empty-default"));
        assertEquals("empty-default", user.getUsername());
    }

    @Test
    @DisplayName("根据默认值 Map 填充属性")
    void shouldFillDefaultsByMap() {
        UserEntity user = new UserEntity();

        BeanUtil.fillDefaultsIfNull(user, Map.of("username", "map-default", "age", 20));
        BeanUtil.fillDefaultsIfEmpty(user, Map.of("username", "new-default"));

        assertEquals("map-default", user.getUsername());
        assertEquals(20, user.getAge());
    }

    @Test
    @DisplayName("预热、读取和清理反射缓存")
    void shouldWarmUpReadAndClearCache() {
        BeanUtil.warmUpCache(UserEntity.class);
        BeanUtil.CacheSnapshot snapshot = BeanUtil.getCacheSnapshot(UserEntity.class);

        assertTrue(snapshot.fieldCount() > 0);
        assertTrue(snapshot.propertyCount() > 0);
        assertTrue(snapshot.methodCount() > 0);
        assertTrue(snapshot.constructorCount() > 0);
        assertTrue(BeanUtil.getCachedFieldCount(UserEntity.class) > 0);
        assertTrue(BeanUtil.getCachedPropertyCount(UserEntity.class) > 0);
        assertTrue(BeanUtil.getCachedMethodCount(UserEntity.class) > 0);
        assertTrue(BeanUtil.getCachedConstructorCount(UserEntity.class) > 0);

        BeanUtil.clearAllCache(UserEntity.class);
        BeanUtil.warmUpCaches(List.of(UserEntity.class));
        BeanUtil.clearAllCaches(List.of(UserEntity.class));
    }

    @Test
    @DisplayName("安全获取和设置属性")
    void shouldSafelyReadAndWriteValues() {
        UserEntity user = new UserEntity();

        assertTrue(BeanUtil.safeSetPropertyValue(user, "username", "safe-user"));
        assertEquals(Optional.of("safe-user"), BeanUtil.safeGetPropertyValue(user, "username"));
        assertEquals(Optional.of("safe-user"), BeanUtil.safeGetPropertyValue(user, "username", String.class));
        assertFalse(BeanUtil.safeSetPropertyValue(user, "notExists", "value"));
        assertTrue(BeanUtil.safeGetPropertyValue(user, "notExists").isEmpty());

        assertTrue(BeanUtil.safeSetFieldValue(user, "id", 1002L));
        assertEquals(Optional.of(1002L), BeanUtil.safeGetFieldValue(user, "id"));
    }

    @Test
    @DisplayName("安全获取和设置嵌套属性")
    void shouldSafelyReadAndWriteNestedValues() {
        UserEntity user = new UserEntity();

        assertTrue(BeanUtil.safeSetNestedPropertyValue(user, "address.city", "广州"));
        assertEquals(Optional.of("广州"), BeanUtil.safeGetNestedPropertyValue(user, "address.city"));
        assertTrue(BeanUtil.safeGetNestedPropertyValue(user, "address.notExists").isEmpty());
    }

    @Test
    @DisplayName("安全执行反射访问和调用方法")
    void shouldSafelyAccessAndInvokeMethod() {
        UserEntity user = new UserEntity(1003L, "invoke-user");

        assertEquals(Optional.of("invoke-user"), BeanUtil.safeAccess(user::getUsername));
        assertTrue(BeanUtil.safeAccess(() -> user.setUsername("changed")));
        assertEquals(Optional.of("changed"), BeanUtil.safeInvokeMethod(user, "getUsername"));
        assertTrue(BeanUtil.safeInvokeMethod(user, "notExists").isEmpty());
    }

    @Test
    @DisplayName("查找方法")
    void shouldFindMethods() {
        Optional<Method> getter = BeanUtil.findMethod(UserEntity.class, "getUsername");
        Optional<Method> setter = BeanUtil.findMethod(UserEntity.class, "setUsername", String.class);
        Optional<Method> compatible = BeanUtil.findCompatibleMethod(UserEntity.class, "setUsername", "value");

        assertTrue(getter.isPresent());
        assertTrue(setter.isPresent());
        assertTrue(compatible.isPresent());
        assertTrue(BeanUtil.getMethods(UserEntity.class).size() > 0);
    }
}
