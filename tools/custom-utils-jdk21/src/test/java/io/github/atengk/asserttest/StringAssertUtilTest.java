package io.github.atengk.asserttest;

import io.github.atengk.utils.AssertUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StringAssertUtilTest {

    @Test
    void shouldPassWhenStringRulesAreValid() {
        assertEquals("abc", AssertUtil.notBlank("abc", "不能为空白"));
        assertEquals(" ", AssertUtil.notEmpty(" ", "不能为空"));
        assertEquals("abc", AssertUtil.hasText("abc", "必须有文本"));
        assertEquals("abc", AssertUtil.length("abc", 3, "长度错误"));
        assertEquals("abc", AssertUtil.minLength("abc", 2, "长度过短"));
        assertEquals("abc", AssertUtil.maxLength("abc", 3, "长度过长"));
        assertEquals("abc", AssertUtil.lengthBetween("abc", 2, 4, "长度不在范围内"));
        assertEquals("abc", AssertUtil.startsWith("abc", "a", "前缀错误"));
        assertEquals("abc", AssertUtil.endsWith("abc", "c", "后缀错误"));
        assertEquals("abc", AssertUtil.contains("abc", "b", "必须包含 b"));
        assertEquals("abc", AssertUtil.notContains("abc", "x", "不能包含 x"));
        assertEquals("A001", AssertUtil.matches("A001", "A\\d{3}", "格式错误"));
        assertEquals("A001", AssertUtil.notMatches("A001", "B\\d{3}", "格式错误"));
    }

    @Test
    void shouldPassWhenStringIsBlankOrEmptyAsExpected() {
        assertDoesNotThrow(() -> AssertUtil.isBlank(" ", "必须为空白"));
        assertDoesNotThrow(() -> AssertUtil.isBlank(null, "必须为空白"));
        assertDoesNotThrow(() -> AssertUtil.isEmpty("", "必须为空"));
        assertDoesNotThrow(() -> AssertUtil.isEmpty((String) null, "必须为空"));
    }

    @Test
    void shouldThrowWhenStringRulesAreInvalid() {
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notBlank(" ", "不能为空白"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notBlank(null, "不能为空白"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notEmpty("", "不能为空"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.isBlank("abc", "必须为空白"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.isEmpty("abc", "必须为空"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.length("abc", 2, "长度错误"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.minLength("abc", 4, "长度过短"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.maxLength("abc", 2, "长度过长"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.lengthBetween("abc", 4, 5, "长度不在范围内"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.startsWith("abc", "x", "前缀错误"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.endsWith("abc", "x", "后缀错误"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.contains("abc", "x", "必须包含 x"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notContains("abc", "b", "不能包含 b"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.matches("A001", "[", "正则错误"));
    }

    @Test
    void shouldThrowWhenLengthArgumentsAreIllegal() {
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.length("abc", -1, "长度错误"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.lengthBetween("abc", 3, 2, "长度范围错误"));
    }
}
