package io.github.atengk.asserttest;

import io.github.atengk.utils.AssertUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnumAssertUtilTest {

    @Test
    void shouldPassWhenEnumRulesAreValid() {
        assertEquals("ENABLED", AssertUtil.enumNameOf("ENABLED", Status.class, "枚举名称错误"));
        assertEquals("1", AssertUtil.enumValueOf("1", List.of("0", "1"), "枚举值错误"));
        assertEquals(Status.ENABLED, AssertUtil.enumIn(Status.ENABLED, List.of(Status.ENABLED), "枚举必须在候选集合中"));
        assertEquals(Status.DISABLED, AssertUtil.enumNotIn(Status.DISABLED, List.of(Status.ENABLED), "枚举不能在候选集合中"));
    }

    @Test
    void shouldThrowWhenEnumRulesAreInvalid() {
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.enumNameOf("UNKNOWN", Status.class, "枚举名称错误"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.enumNameOf(" ", Status.class, "枚举名称错误"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.enumValueOf("9", List.of("0", "1"), "枚举值错误"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.enumIn(Status.DISABLED, List.of(Status.ENABLED), "枚举必须在候选集合中"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.enumNotIn(Status.ENABLED, List.of(Status.ENABLED), "枚举不能在候选集合中"));
    }

    enum Status {
        ENABLED,
        DISABLED
    }
}
