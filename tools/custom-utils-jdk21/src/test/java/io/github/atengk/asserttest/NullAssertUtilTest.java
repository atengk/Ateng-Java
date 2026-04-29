package io.github.atengk.asserttest;

import io.github.atengk.utils.AssertUtil;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NullAssertUtilTest {

    @Test
    void shouldPassWhenNullRulesAreValid() {
        Object value = new Object();

        assertSame(value, AssertUtil.notNull(value, "对象不能为空"));
        assertDoesNotThrow(() -> AssertUtil.isNull(null, "对象必须为空"));
        assertDoesNotThrow(() -> AssertUtil.allNotNull("对象不能包含空值", "a", 1, value));
        assertDoesNotThrow(() -> AssertUtil.anyNotNull("至少一个对象不能为空", null, "a", null));
        assertDoesNotThrow(() -> AssertUtil.allNull("所有对象必须为空", null, null));
    }

    @Test
    void shouldThrowWhenNullRulesAreInvalid() {
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notNull(null, "对象不能为空"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.isNull("value", "对象必须为空"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.allNotNull("对象不能包含空值", "a", null));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.anyNotNull("至少一个对象不能为空", null, null));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.allNull("所有对象必须为空", null, "a"));
    }

    @Test
    void shouldSupportOptionalAssertions() {
        assertDoesNotThrow(() -> AssertUtil.notEmpty(Optional.of("value"), "Optional 必须有值"));
        assertDoesNotThrow(() -> AssertUtil.isEmpty(Optional.empty(), "Optional 必须为空"));
        assertDoesNotThrow(() -> AssertUtil.isEmpty((Optional<?>) null, "Optional 必须为空"));

        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notEmpty(Optional.empty(), "Optional 必须有值"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notEmpty((Optional<String>) null, "Optional 必须有值"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.isEmpty(Optional.of("value"), "Optional 必须为空"));
    }
}
