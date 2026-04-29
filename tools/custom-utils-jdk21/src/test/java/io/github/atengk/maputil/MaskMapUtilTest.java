package io.github.atengk.maputil;

import io.github.atengk.utils.MapUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class MaskMapUtilTest {

    @Test
    void shouldMaskSelectedKeys() {
        Map<String, Object> map = Map.of("name", "Ateng", "age", 18);

        Map<String, Object> masked = MapUtil.mask(map, List.of("name"));
        assertEquals("A***g", masked.get("name"));
        assertEquals(18, masked.get("age"));
        assertEquals("Ateng", map.get("name"));
    }

    @Test
    void shouldMaskWithCustomRulesAndRemoveKeys() {
        Map<String, Object> map = Map.of("token", "abcdef", "name", "Ateng");
        Map<String, Function<Object, Object>> rules = Map.of("token", value -> "***");

        assertEquals("***", MapUtil.mask(map, rules).get("token"));
        assertFalse(MapUtil.removeSensitiveKeys(map, List.of("token")).containsKey("token"));
        assertFalse(MapUtil.copyWithoutSensitiveKeys(map, List.of("token")).containsKey("token"));
    }

    @Test
    void shouldMaskPhoneEmailAndIdCard() {
        Map<String, Object> map = Map.of(
                "phone", "13812345678",
                "email", "ateng@example.com",
                "id", "110101199001011234"
        );

        assertEquals("138****5678", MapUtil.maskPhone(map, "phone").get("phone"));
        assertEquals("a***g@example.com", MapUtil.maskEmail(map, "email").get("email"));
        assertEquals("110101********1234", MapUtil.maskIdCard(map, "id").get("id"));
    }
}
