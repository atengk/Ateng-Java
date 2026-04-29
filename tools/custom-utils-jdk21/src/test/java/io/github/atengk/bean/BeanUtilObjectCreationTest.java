package io.github.atengk.bean;

import io.github.atengk.utils.BeanUtil;
import io.github.atengk.bean.fixture.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BeanUtil 对象创建单元测试。
 *
 * @author Ateng
 * @since 2026-04-29
 */
@DisplayName("BeanUtil 对象创建")
class BeanUtilObjectCreationTest {

    @Test
    @DisplayName("使用无参构造创建对象")
    void shouldCreateBeanWithNoArgsConstructor() {
        UserEntity user = BeanUtil.newInstance(UserEntity.class);

        assertNotNull(user);
        assertNull(user.getId());
    }

    @Test
    @DisplayName("使用指定构造参数类型创建对象")
    void shouldCreateBeanWithDeclaredConstructor() {
        UserEntity user = BeanUtil.newInstance(UserEntity.class, new Class<?>[]{Long.class, String.class}, 1001L, "ateng");

        assertEquals(1001L, user.getId());
        assertEquals("ateng", user.getUsername());
    }

    @Test
    @DisplayName("根据参数值自动匹配构造方法创建对象")
    void shouldCreateBeanWithCompatibleConstructor() {
        UserEntity user = BeanUtil.newInstance(UserEntity.class, 1002L, "admin");

        assertEquals(1002L, user.getId());
        assertEquals("admin", user.getUsername());
    }

    @Test
    @DisplayName("安全创建对象失败时返回空")
    void shouldReturnEmptyWhenTryCreateInvalidClass() {
        Optional<Runnable> result = BeanUtil.tryNewInstance(Runnable.class);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("判断类型是否可以实例化")
    void shouldCheckInstantiableClass() {
        assertTrue(BeanUtil.isInstantiable(UserEntity.class));
        assertFalse(BeanUtil.isInstantiable(Runnable.class));
        assertFalse(BeanUtil.isInstantiable(int.class));
    }

    @Test
    @DisplayName("获取构造方法列表")
    void shouldGetConstructors() {
        List<Constructor<?>> constructors = BeanUtil.getConstructors(UserEntity.class);

        assertTrue(constructors.size() >= 2);
    }

    @Test
    @DisplayName("判断是否存在无参构造方法")
    void shouldCheckNoArgsConstructor() {
        assertTrue(BeanUtil.hasNoArgsConstructor(UserEntity.class));
        assertFalse(BeanUtil.hasNoArgsConstructor(Runnable.class));
    }
}
