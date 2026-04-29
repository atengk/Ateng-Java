package io.github.atengk.asserttest;

import io.github.atengk.utils.AssertUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TypeAssertUtilTest {

    @Test
    void shouldPassWhenTypeRulesAreValid() {
        String value = "abc";

        assertSame(value, AssertUtil.instanceOf(value, String.class, "类型错误"));
        assertSame(value, AssertUtil.notInstanceOf(value, Integer.class, "类型不能匹配"));
        assertDoesNotThrow(() -> AssertUtil.assignableFrom(List.class, ArrayList.class, "类型不可赋值"));
        assertDoesNotThrow(() -> AssertUtil.notAssignableFrom(String.class, Integer.class, "类型不能赋值"));
        assertDoesNotThrow(() -> AssertUtil.sameType("a", "b", "类型必须一致"));
        assertDoesNotThrow(() -> AssertUtil.notSameType("a", 1, "类型不能一致"));
    }

    @Test
    void shouldThrowWhenTypeRulesAreInvalid() {
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.instanceOf("abc", Integer.class, "类型错误"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notInstanceOf("abc", String.class, "类型不能匹配"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.assignableFrom(ArrayList.class, List.class, "类型不可赋值"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notAssignableFrom(List.class, ArrayList.class, "类型不能赋值"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.sameType("a", 1, "类型必须一致"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notSameType("a", "b", "类型不能一致"));
    }

    @Test
    void shouldThrowWhenTypeArgumentsAreIllegal() {
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.instanceOf("abc", null, "类型不能为空"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.assignableFrom(null, String.class, "父类型不能为空"));
    }
}
