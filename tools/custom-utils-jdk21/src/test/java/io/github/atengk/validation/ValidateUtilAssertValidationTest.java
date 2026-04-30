package io.github.atengk.validation;

import io.github.atengk.utils.validation.ValidateException;
import io.github.atengk.utils.validation.ValidateUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateUtilAssertValidationTest {

    @Test
    void assertMethodsShouldPassForValidValues() {
        assertEquals("a", ValidateUtil.notNull("a", "不能为空"));
        assertEquals("a", ValidateUtil.notBlank("a", "不能为空"));
        assertEquals(List.of("a"), ValidateUtil.notEmpty(List.of("a"), "集合不能为空"));
        assertEquals(Map.of("k", "v"), ValidateUtil.notEmpty(Map.of("k", "v"), "Map不能为空"));
        assertDoesNotThrow(() -> ValidateUtil.isTrue(true, "必须为真"));
        assertDoesNotThrow(() -> ValidateUtil.isFalse(false, "必须为假"));
        assertDoesNotThrow(() -> ValidateUtil.equals("a", "a", "必须相等"));
        assertDoesNotThrow(() -> ValidateUtil.notEquals("a", "b", "不能相等"));
        assertEquals("a", ValidateUtil.in("a", List.of("a", "b"), "必须在范围内"));
        assertEquals("c", ValidateUtil.notIn("c", List.of("a", "b"), "不能在范围内"));
    }

    @Test
    void assertMethodsShouldThrowForInvalidValues() {
        assertThrows(ValidateException.class, () -> ValidateUtil.notNull(null, "不能为空"));
        assertThrows(ValidateException.class, () -> ValidateUtil.notBlank(" ", "不能为空"));
        assertThrows(ValidateException.class, () -> ValidateUtil.notEmpty(List.of(), "集合不能为空"));
        assertThrows(ValidateException.class, () -> ValidateUtil.notEmpty(Map.of(), "Map不能为空"));
        assertThrows(ValidateException.class, () -> ValidateUtil.isTrue(false, "必须为真"));
        assertThrows(ValidateException.class, () -> ValidateUtil.isFalse(true, "必须为假"));
        assertThrows(ValidateException.class, () -> ValidateUtil.equals("a", "b", "必须相等"));
        assertThrows(ValidateException.class, () -> ValidateUtil.notEquals("a", "a", "不能相等"));
        assertThrows(ValidateException.class, () -> ValidateUtil.in("c", List.of("a", "b"), "必须在范围内"));
        assertThrows(ValidateException.class, () -> ValidateUtil.notIn("a", List.of("a", "b"), "不能在范围内"));
    }

    @Test
    void assertMethodsShouldUseDefaultMessageWhenMessageIsBlank() {
        ValidateException exception = assertThrows(ValidateException.class, () -> ValidateUtil.notBlank("", " "));

        assertEquals("字符串不能为空", exception.getMessage());
    }
}
