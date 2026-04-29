package io.github.atengk.asserttest;

import io.github.atengk.utils.AssertUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BasicAssertUtilTest {

    @Test
    void shouldPassWhenBasicConditionsAreValid() {
        assertDoesNotThrow(() -> AssertUtil.isTrue(1 < 2, "条件必须成立"));
        assertDoesNotThrow(() -> AssertUtil.isFalse(1 > 2, "条件必须不成立"));
        assertDoesNotThrow(() -> AssertUtil.valid("abc".startsWith("a"), "业务条件不满足"));
        assertDoesNotThrow(() -> AssertUtil.state(true, "状态错误"));
        assertDoesNotThrow(() -> AssertUtil.failIf(false, "不应该失败"));
    }

    @Test
    void shouldThrowWhenBasicConditionsAreInvalid() {
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.isTrue(false, "条件必须成立"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.isFalse(true, "条件必须不成立"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.valid(false, "业务条件不满足"));
        assertThrows(IllegalStateException.class, () -> AssertUtil.state(false, "状态错误"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.fail("主动失败"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.failIf(true, "条件成立时失败"));
    }

    @Test
    void shouldSupportFailIfNullAndFailIfPresent() {
        assertDoesNotThrow(() -> AssertUtil.failIfNull("value", "不能为空"));
        assertDoesNotThrow(() -> AssertUtil.failIfPresent(null, "必须为空"));

        assertThrows(IllegalArgumentException.class, () -> AssertUtil.failIfNull(null, "不能为空"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.failIfPresent("value", "必须为空"));
    }
}
