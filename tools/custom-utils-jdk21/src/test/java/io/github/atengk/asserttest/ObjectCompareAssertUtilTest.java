package io.github.atengk.asserttest;

import io.github.atengk.utils.AssertUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObjectCompareAssertUtilTest {

    @Test
    void shouldPassWhenObjectCompareRulesAreValid() {
        Object value = new Object();

        assertSame(value, AssertUtil.equalsTo(value, value, "必须相等"));
        assertSame(value, AssertUtil.notEquals(value, new Object(), "不能相等"));
        assertSame(value, AssertUtil.same(value, value, "必须同一引用"));
        assertSame(value, AssertUtil.notSame(value, new Object(), "不能同一引用"));
        assertSame("a", AssertUtil.in("a", List.of("a", "b"), "必须在候选集合中"));
        assertSame("x", AssertUtil.notIn("x", List.of("a", "b"), "不能在候选集合中"));
        assertSame("a", AssertUtil.oneOf("a", "必须是候选值之一", "a", "b"));
        assertSame("x", AssertUtil.notOneOf("x", "不能是候选值之一", "a", "b"));
    }

    @Test
    void shouldThrowWhenObjectCompareRulesAreInvalid() {
        Object value = new Object();

        assertThrows(IllegalArgumentException.class, () -> AssertUtil.equalsTo("a", "b", "必须相等"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notEquals("a", "a", "不能相等"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.same(value, new Object(), "必须同一引用"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notSame(value, value, "不能同一引用"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.in("x", List.of("a", "b"), "必须在候选集合中"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notIn("a", List.of("a", "b"), "不能在候选集合中"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.oneOf("x", "必须是候选值之一", "a", "b"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notOneOf("a", "不能是候选值之一", "a", "b"));
    }
}
