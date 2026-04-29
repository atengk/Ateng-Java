package io.github.atengk.object;

import io.github.atengk.utils.ObjectUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilStateTest {

    @Test
    void shouldCheckBooleanState() {
        assertTrue(ObjectUtil.isTrue(Boolean.TRUE));
        assertFalse(ObjectUtil.isTrue(null));
        assertTrue(ObjectUtil.isFalse(Boolean.FALSE));
        assertFalse(ObjectUtil.isFalse(null));
        assertTrue(ObjectUtil.isNotTrue(null));
        assertTrue(ObjectUtil.isNotFalse(null));
    }

    @Test
    void shouldCheckDefaultValue() {
        assertTrue(ObjectUtil.isDefaultValue(null));
        assertTrue(ObjectUtil.isDefaultValue(false));
        assertTrue(ObjectUtil.isDefaultValue('\0'));
        assertTrue(ObjectUtil.isDefaultValue(0));
        assertTrue(ObjectUtil.isDefaultValue(""));
        assertTrue(ObjectUtil.isDefaultValue(List.of()));
        assertFalse(ObjectUtil.isDefaultValue("a"));
        assertFalse(ObjectUtil.isDefaultValue(1));
    }

    @Test
    void shouldCheckNumberState() {
        assertTrue(ObjectUtil.isZero(BigDecimal.ZERO));
        assertTrue(ObjectUtil.isZero(new BigDecimal("0.00")));
        assertTrue(ObjectUtil.isPositive(1));
        assertTrue(ObjectUtil.isNegative(-1));
        assertFalse(ObjectUtil.isZero(null));
        assertFalse(ObjectUtil.isPositive(Double.NaN));
        assertFalse(ObjectUtil.isNegative(Double.NaN));
    }
}
