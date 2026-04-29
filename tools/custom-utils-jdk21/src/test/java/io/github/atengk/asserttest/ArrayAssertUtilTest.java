package io.github.atengk.asserttest;

import io.github.atengk.utils.AssertUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArrayAssertUtilTest {

    @Test
    void shouldPassWhenArrayRulesAreValid() {
        String[] values = {"a", "b", "c"};

        assertSame(values, AssertUtil.notEmpty(values, "数组不能为空"));
        assertDoesNotThrow(() -> AssertUtil.isEmpty(new String[0], "数组必须为空"));
        assertSame(values, AssertUtil.length(values, 3, "数组长度错误"));
        assertSame(values, AssertUtil.minLength(values, 2, "数组太短"));
        assertSame(values, AssertUtil.maxLength(values, 3, "数组太长"));
        assertSame(values, AssertUtil.lengthBetween(values, 2, 4, "数组长度不在范围内"));
        assertSame(values, AssertUtil.noNullElements(values, "不能包含 null"));
        assertSame(values, AssertUtil.contains(values, "b", "必须包含元素"));
        assertSame(values, AssertUtil.notContains(values, "x", "不能包含元素"));
    }

    @Test
    void shouldThrowWhenArrayRulesAreInvalid() {
        String[] values = {"a", "b", "c"};

        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notEmpty(new String[0], "数组不能为空"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.isEmpty(values, "数组必须为空"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.length(values, 2, "数组长度错误"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.minLength(values, 4, "数组太短"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.maxLength(values, 2, "数组太长"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.lengthBetween(values, 4, 5, "数组长度不在范围内"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.noNullElements(new String[]{"a", null}, "不能包含 null"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.contains(values, "x", "必须包含元素"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notContains(values, "b", "不能包含元素"));
    }

    @Test
    void shouldThrowWhenArrayArgumentsAreIllegal() {
        String[] values = {"a"};

        assertThrows(IllegalArgumentException.class, () -> AssertUtil.length(values, -1, "数组长度错误"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.lengthBetween(values, 2, 1, "数组长度范围错误"));
    }
}
