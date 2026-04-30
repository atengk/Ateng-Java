package io.github.atengk.desensitized;

import io.github.atengk.utils.desensitized.DesensitizedUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutoStrategyDesensitizedTest {

    @AfterEach
    void cleanup() {
        DesensitizedUtil.removeRule("testRule");
    }

    @Test
    void shouldAutoMaskAndDispatchByType() {
        assertEquals("138****5678", DesensitizedUtil.auto("13812345678"));
        assertEquals("t**t@example.com", DesensitizedUtil.auto("test@example.com"));
        assertEquals("https://a.com?token=***&name=tom", DesensitizedUtil.auto("https://a.com?token=abc&name=tom"));
        assertEquals("138****5678", DesensitizedUtil.byType("13812345678", DesensitizedUtil.DesensitizedType.MOBILE));
        assertEquals("110***********1234", DesensitizedUtil.byType("110101199001011234", DesensitizedUtil.DesensitizedType.ID_CARD));
        assertEquals("138****5678", DesensitizedUtil.byFieldName("13812345678", "mobile"));
    }

    @Test
    void shouldMaskByRuleRegexAndStrategy() {
        assertEquals("ab##ef", DesensitizedUtil.byRule("abcdef", 2, 2, '#'));
        assertEquals("abc123***", DesensitizedUtil.byRegex("abc123456", "\\d+", 3, 0));
        assertEquals("fixed", DesensitizedUtil.byStrategy("abc", value -> "fixed"));

        DesensitizedUtil.registerRule("testRule", value -> DesensitizedUtil.mask(value, 1, 1));
        assertNotNull(DesensitizedUtil.getRule("testRule"));
        assertEquals("a*c", DesensitizedUtil.byRule("abc", "testRule"));
        assertTrue(DesensitizedUtil.removeRule("testRule"));
    }

    @Test
    void shouldHandleAutoStrategyException() {
        assertThrows(NullPointerException.class, () -> DesensitizedUtil.byType("abc", null));
        assertThrows(IllegalArgumentException.class, () -> DesensitizedUtil.byRule("abc", ""));
        assertThrows(IllegalArgumentException.class, () -> DesensitizedUtil.byRule("abc", "missing"));
        assertThrows(NullPointerException.class, () -> DesensitizedUtil.byStrategy("abc", null));
        assertThrows(IllegalArgumentException.class, () -> DesensitizedUtil.registerRule("", value -> value));
        assertThrows(NullPointerException.class, () -> DesensitizedUtil.registerRule("testRule", null));
    }
}
