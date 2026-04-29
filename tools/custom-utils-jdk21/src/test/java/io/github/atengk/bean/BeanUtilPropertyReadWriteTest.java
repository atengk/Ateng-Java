package io.github.atengk.bean;

import io.github.atengk.utils.BeanUtil;
import io.github.atengk.bean.fixture.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BeanUtil 属性读取和属性写入单元测试。
 *
 * @author Ateng
 * @since 2026-04-29
 */
@DisplayName("BeanUtil 属性读取和属性写入")
class BeanUtilPropertyReadWriteTest {

    @Test
    @DisplayName("直接读取和写入字段")
    void shouldReadAndWriteFieldValue() {
        UserEntity user = new UserEntity();

        BeanUtil.setFieldValue(user, "id", 1001L);

        assertEquals(1001L, BeanUtil.getFieldValue(user, "id"));
    }

    @Test
    @DisplayName("通过属性名读取和写入属性")
    void shouldReadAndWritePropertyValue() {
        UserEntity user = new UserEntity();

        BeanUtil.setPropertyValue(user, "username", "ateng");
        BeanUtil.setPropertyValue(user, "age", 25);

        assertEquals("ateng", BeanUtil.getPropertyValue(user, "username"));
        assertEquals(25, BeanUtil.getPropertyValue(user, "age", Integer.class));
    }

    @Test
    @DisplayName("读取属性为空时返回默认值")
    void shouldReturnDefaultValueWhenPropertyIsNull() {
        UserEntity user = new UserEntity();

        String username = BeanUtil.getPropertyValueOrDefault(user, "username", "default-user");

        assertEquals("default-user", username);
    }

    @Test
    @DisplayName("读取所有可读属性")
    void shouldGetReadablePropertyValues() {
        UserEntity user = new UserEntity(1002L, "admin");
        user.setAge(30);

        Map<String, Object> values = BeanUtil.getReadablePropertyValues(user);

        assertEquals(1002L, values.get("id"));
        assertEquals("admin", values.get("username"));
        assertEquals(30, values.get("age"));
    }

    @Test
    @DisplayName("属性存在时才写入")
    void shouldSetPropertyValueIfPresent() {
        UserEntity user = new UserEntity();

        assertTrue(BeanUtil.setPropertyValueIfPresent(user, "username", "tester"));
        assertFalse(BeanUtil.setPropertyValueIfPresent(user, "notExists", "value"));
        assertEquals("tester", user.getUsername());
    }

    @Test
    @DisplayName("批量设置属性")
    void shouldSetPropertyValues() {
        UserEntity user = new UserEntity();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", 1003L);
        values.put("username", "batch-user");
        values.put("age", 28);
        values.put("unknown", "ignored");

        BeanUtil.setPropertyValues(user, values, true, false);

        assertEquals(1003L, user.getId());
        assertEquals("batch-user", user.getUsername());
        assertEquals(28, user.getAge());
    }

    @Test
    @DisplayName("写入 final 字段时抛出异常")
    void shouldThrowWhenWriteFinalField() {
        UserEntity user = new UserEntity();

        assertThrows(BeanUtil.BeanException.class, () -> BeanUtil.setFieldValue(user, "immutableCode", "NEW"));
    }
}
