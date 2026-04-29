package io.github.atengk.maputil;

import io.github.atengk.utils.MapUtil;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BeanMapUtilTest {

    @Test
    void shouldConvertBeanAndMap() {
        User user = new User();
        user.name = "Ateng";
        user.age = 18;
        user.remark = null;

        Map<String, Object> map = MapUtil.beanToMap(user);
        assertEquals("Ateng", map.get("name"));
        assertEquals(18, map.get("age"));
        assertTrue(map.containsKey("remark"));
        assertFalse(MapUtil.beanToMap(user, true).containsKey("remark"));

        User copied = MapUtil.mapToBean(Map.of("name", "Blair", "age", "20"), User.class);
        assertEquals("Blair", copied.name);
        assertEquals(20, copied.age);
    }

    @Test
    void shouldCopyBeanValues() {
        User user = new User();
        MapUtil.copyToBean(Map.of("name", "Ateng", "age", 21), user);
        assertEquals("Ateng", user.name);
        assertEquals(21, user.age);

        Map<String, Object> target = new LinkedHashMap<>();
        MapUtil.copyFromBean(user, target);
        assertEquals("Ateng", target.get("name"));
        assertEquals("21", MapUtil.beanToStringMap(user).get("age"));
    }

    @Test
    void shouldHandleConversionErrorPolicies() {
        assertThrows(IllegalArgumentException.class, () -> MapUtil.mapToBean(Map.of("age", "bad"), User.class));

        User ignored = MapUtil.mapToBeanIgnoreError(Map.of("age", "bad", "name", "Ateng"), User.class);
        assertEquals("Ateng", ignored.name);
        assertEquals(0, ignored.age);
        assertThrows(NullPointerException.class, () -> MapUtil.copyToBean(Map.of(), null));
        assertThrows(IllegalArgumentException.class, () -> MapUtil.mapToBean(Map.of(), NoNoArgs.class));
    }

    private static class User {
        private String name;
        private int age;
        private String remark;
    }

    private static class NoNoArgs {
        private NoNoArgs(String name) {
        }
    }
}
