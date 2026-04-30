package io.github.atengk.security;

import io.github.atengk.utils.security.SecurityUtil;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilMaskTest {

    @Test
    void maskCommonDataShouldWork() {
        assertEquals("138****5678", SecurityUtil.maskMobile("13812345678"));
        assertEquals("a****@example.com", SecurityUtil.maskEmail("ateng@example.com"));
        assertEquals("张*", SecurityUtil.maskName("张三"));
        assertEquals("192.168.*.*", SecurityUtil.maskIp("192.168.1.2"));
        assertEquals("******", SecurityUtil.maskPassword("secret"));
        assertEquals("abc****xyz", SecurityUtil.mask("abcdefgxyz", 3, 3));
    }

    @Test
    void maskJsonAndMapShouldWork() {
        String json = "{\"mobile\":\"13812345678\",\"name\":\"Ateng\"}";
        assertTrue(SecurityUtil.maskJson(json, Set.of("mobile")).contains("1*********8"));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("token", "abcdef");
        assertEquals("a****f", SecurityUtil.maskMap(map, Set.of("token")).get("token"));
        assertEquals("a**z", SecurityUtil.maskByPattern("abcz", Pattern.compile("abcz")));
    }

    @Test
    void maskShouldHandleBoundary() {
        assertEquals("", SecurityUtil.mask(null, 1, 1));
        assertEquals("***", SecurityUtil.mask("abc", 3, 0));
        assertThrows(IllegalArgumentException.class, () -> SecurityUtil.mask("abc", -1, 0));
    }
}
