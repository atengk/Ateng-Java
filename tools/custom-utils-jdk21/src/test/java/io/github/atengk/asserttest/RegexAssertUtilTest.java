package io.github.atengk.asserttest;

import io.github.atengk.utils.AssertUtil;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegexAssertUtilTest {

    @Test
    void shouldPassWhenRegexRulesAreValid() {
        Pattern codePattern = Pattern.compile("A\\d{3}");

        assertEquals("A\\d{3}", AssertUtil.validPattern("A\\d{3}", "正则必须合法"));
        assertEquals("A001", AssertUtil.format("A001", codePattern, "编码格式错误"));
        assertEquals("B001", AssertUtil.notFormat("B001", codePattern, "编码格式不能匹配"));
        assertEquals("user@example.com", AssertUtil.email("user@example.com", "邮箱格式错误"));
        assertEquals("https://example.com", AssertUtil.url("https://example.com", "URL 格式错误"));
        assertEquals("550e8400-e29b-41d4-a716-446655440000", AssertUtil.uuid("550e8400-e29b-41d4-a716-446655440000", "UUID 格式错误"));
    }

    @Test
    void shouldThrowWhenRegexRulesAreInvalid() {
        Pattern codePattern = Pattern.compile("A\\d{3}");

        assertThrows(IllegalArgumentException.class, () -> AssertUtil.validPattern("[", "正则必须合法"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.validPattern(null, "正则必须合法"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.format("B001", codePattern, "编码格式错误"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.format(null, codePattern, "编码格式错误"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notFormat("A001", codePattern, "编码格式不能匹配"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.format("A001", null, "Pattern 不能为空"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.email("user", "邮箱格式错误"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.url("ftp://example.com", "URL 格式错误"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.uuid("bad", "UUID 格式错误"));
    }
}
