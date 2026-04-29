package io.github.atengk.asserttest;

import io.github.atengk.utils.AssertUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NumberAssertUtilTest {

    @Test
    void shouldPassWhenNumberRulesAreValid() {
        assertEquals(1, AssertUtil.positive(1, "必须大于 0"));
        assertEquals(-1, AssertUtil.negative(-1, "必须小于 0"));
        assertEquals(0, AssertUtil.nonPositive(0, "必须小于等于 0"));
        assertEquals(0, AssertUtil.nonNegative(0, "必须大于等于 0"));
        assertEquals(0, AssertUtil.zero(0, "必须等于 0"));
        assertEquals(1, AssertUtil.notZero(1, "不能等于 0"));
        assertEquals(2, AssertUtil.greaterThan(2, 1, "必须大于指定值"));
        assertEquals(2, AssertUtil.greaterThanOrEqual(2, 2, "必须大于等于指定值"));
        assertEquals(1, AssertUtil.lessThan(1, 2, "必须小于指定值"));
        assertEquals(2, AssertUtil.lessThanOrEqual(2, 2, "必须小于等于指定值"));
        assertEquals(new BigDecimal("10.50"), AssertUtil.between(new BigDecimal("10.50"), 1, 20, "必须在范围内"));
        assertEquals(30, AssertUtil.notBetween(30, 1, 20, "不能在范围内"));
    }

    @Test
    void shouldThrowWhenNumberRulesAreInvalid() {
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.positive(0, "必须大于 0"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.negative(0, "必须小于 0"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.nonPositive(1, "必须小于等于 0"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.nonNegative(-1, "必须大于等于 0"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.zero(1, "必须等于 0"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notZero(0, "不能等于 0"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.greaterThan(1, 1, "必须大于指定值"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.greaterThanOrEqual(1, 2, "必须大于等于指定值"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.lessThan(2, 2, "必须小于指定值"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.lessThanOrEqual(3, 2, "必须小于等于指定值"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.between(30, 1, 20, "必须在范围内"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notBetween(10, 1, 20, "不能在范围内"));
    }

    @Test
    void shouldThrowWhenNumberArgumentsAreIllegal() {
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.positive(null, "不能为空"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.positive(Double.NaN, "不能是 NaN"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.positive(Double.POSITIVE_INFINITY, "不能是 Infinity"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.between(1, 10, 1, "范围错误"));
    }
}
