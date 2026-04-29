package io.github.atengk.asserttest;

import io.github.atengk.utils.AssertUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CollectionAssertUtilTest {

    @Test
    void shouldPassWhenCollectionRulesAreValid() {
        List<Integer> values = List.of(1, 2, 3);

        assertSame(values, AssertUtil.notEmpty(values, "集合不能为空"));
        assertDoesNotThrow(() -> AssertUtil.isEmpty(List.of(), "集合必须为空"));
        assertSame(values, AssertUtil.size(values, 3, "集合大小错误"));
        assertSame(values, AssertUtil.minSize(values, 2, "集合太小"));
        assertSame(values, AssertUtil.maxSize(values, 3, "集合太大"));
        assertSame(values, AssertUtil.sizeBetween(values, 2, 4, "集合大小不在范围内"));
        assertSame(values, AssertUtil.contains(values, 2, "必须包含元素"));
        assertSame(values, AssertUtil.notContains(values, 9, "不能包含元素"));
        assertSame(values, AssertUtil.noNullElements(values, "不能包含 null"));
        assertSame(values, AssertUtil.allMatch(values, item -> item > 0, "必须全部大于 0"));
        assertSame(values, AssertUtil.anyMatch(values, item -> item == 2, "至少一个等于 2"));
        assertSame(values, AssertUtil.noneMatch(values, item -> item < 0, "不能小于 0"));
    }

    @Test
    void shouldPassWhenCollectionHasNullElementExpected() {
        List<String> values = Arrays.asList("a", null, "b");
        assertSame(values, AssertUtil.hasNullElements(values, "必须包含 null"));
    }

    @Test
    void shouldThrowWhenCollectionRulesAreInvalid() {
        List<Integer> values = List.of(1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notEmpty(List.of(), "集合不能为空"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.isEmpty(values, "集合必须为空"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.size(values, 2, "集合大小错误"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.minSize(values, 4, "集合太小"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.maxSize(values, 2, "集合太大"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.sizeBetween(values, 4, 5, "集合大小不在范围内"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.contains(values, 9, "必须包含元素"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.notContains(values, 2, "不能包含元素"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.noNullElements(Arrays.asList("a", null), "不能包含 null"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.hasNullElements(List.of("a"), "必须包含 null"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.allMatch(values, item -> item > 1, "必须全部大于 1"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.anyMatch(values, item -> item == 9, "至少一个等于 9"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.noneMatch(values, item -> item == 2, "不能等于 2"));
    }

    @Test
    void shouldThrowWhenCollectionArgumentsAreIllegal() {
        List<Integer> values = List.of(1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> AssertUtil.size(values, -1, "集合大小错误"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.sizeBetween(values, 3, 2, "集合大小范围错误"));
        assertThrows(IllegalArgumentException.class, () -> AssertUtil.allMatch(values, null, "predicate 不能为空"));
    }
}
