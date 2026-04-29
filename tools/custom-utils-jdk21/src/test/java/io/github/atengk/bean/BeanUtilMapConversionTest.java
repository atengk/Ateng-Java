package io.github.atengk.bean;

import io.github.atengk.utils.BeanUtil;
import io.github.atengk.bean.fixture.LoginStatus;
import io.github.atengk.bean.fixture.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BeanUtil Bean 转 Map 和 Map 转 Bean 单元测试。
 *
 * @author Ateng
 * @since 2026-04-29
 */
@DisplayName("BeanUtil Bean 转 Map 和 Map 转 Bean")
class BeanUtilMapConversionTest {

    @Test
    @DisplayName("Bean 转 Map 保留空值")
    void shouldConvertBeanToMap() {
        UserEntity user = new UserEntity(1001L, "ateng");
        user.setAge(25);

        Map<String, Object> map = BeanUtil.toMap(user);

        assertEquals(1001L, map.get("id"));
        assertEquals("ateng", map.get("username"));
        assertTrue(map.containsKey("enabled"));
    }

    @Test
    @DisplayName("Bean 转 Map 忽略空值")
    void shouldConvertBeanToMapIgnoringNull() {
        UserEntity user = new UserEntity(1002L, "admin");

        Map<String, Object> map = BeanUtil.toMap(user, BeanUtil.BeanToMapOptions.ofIgnoreNull());

        assertEquals(1002L, map.get("id"));
        assertFalse(map.containsKey("age"));
    }

    @Test
    @DisplayName("Bean 转 Map 使用包含和排除属性")
    void shouldConvertBeanToMapWithIncludeAndExcludeProperties() {
        UserEntity user = new UserEntity(1003L, "tester");
        user.setAge(30);

        Map<String, Object> map = BeanUtil.toMap(user, false, List.of("id", "username", "age"), List.of("age"));

        assertEquals(2, map.size());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("username"));
        assertFalse(map.containsKey("age"));
    }

    @Test
    @DisplayName("Map 转 Bean")
    void shouldConvertMapToBean() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", 1004L);
        map.put("username", "map-user");
        map.put("age", 18);
        map.put("enabled", true);
        map.put("status", LoginStatus.ENABLED);
        map.put("birthday", LocalDate.of(2026, 4, 29));

        UserEntity user = BeanUtil.toBean(map, UserEntity.class);

        assertEquals(1004L, user.getId());
        assertEquals("map-user", user.getUsername());
        assertEquals(18, user.getAge());
        assertEquals(LoginStatus.ENABLED, user.getStatus());
    }

    @Test
    @DisplayName("Map 转 Bean 忽略未知属性")
    void shouldConvertMapToBeanIgnoringUnknownProperty() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", 1005L);
        map.put("username", "unknown-user");
        map.put("unknown", "ignored");

        UserEntity user = BeanUtil.toBean(map, UserEntity.class, true);

        assertEquals(1005L, user.getId());
        assertEquals("unknown-user", user.getUsername());
    }

    @Test
    @DisplayName("Map 填充 Bean 时忽略空值")
    void shouldFillBeanIgnoringNull() {
        UserEntity user = new UserEntity(1006L, "before");
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("username", null);
        map.put("age", 33);

        BeanUtil.fillBean(map, user, true, true);

        assertEquals("before", user.getUsername());
        assertEquals(33, user.getAge());
    }

    @Test
    @DisplayName("Map 和 Bean 转换别名方法")
    void shouldUseAliasMethods() {
        UserEntity user = new UserEntity(1007L, "alias-user");
        Map<String, Object> beanMap = BeanUtil.beanToMap(user);
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put("id", beanMap.get("id"));
        sourceMap.put("username", beanMap.get("username"));

        UserEntity copied = BeanUtil.mapToBean(sourceMap, UserEntity.class);

        assertEquals(user.getId(), copied.getId());
        assertEquals(user.getUsername(), copied.getUsername());
    }
}
