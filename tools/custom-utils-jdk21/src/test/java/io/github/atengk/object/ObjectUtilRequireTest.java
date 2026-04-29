package io.github.atengk.object;

import io.github.atengk.utils.ObjectUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilRequireTest {

    @Test
    void shouldRequireNonNullAndNonEmpty() {
        assertEquals("a", ObjectUtil.requireNonNull("a"));
        assertEquals("a", ObjectUtil.requireNonNull("a", "不能为空"));
        assertEquals("a", ObjectUtil.requireNonEmpty("a", "不能为空"));
        assertEquals("a", ObjectUtil.requireNonBlank("a", "不能为空"));
    }

    @Test
    void shouldThrowForInvalidValues() {
        assertThrows(NullPointerException.class, () -> ObjectUtil.requireNonNull(null));
        assertThrows(IllegalArgumentException.class, () -> ObjectUtil.requireNonEmpty(List.of(), "不能为空"));
        assertThrows(IllegalArgumentException.class, () -> ObjectUtil.requireNonBlank(" ", "不能为空"));
    }

    @Test
    void shouldRequireBatchValues() {
        assertDoesNotThrow(() -> ObjectUtil.requireAllNonNull("不能为空", "a", 1));
        assertDoesNotThrow(() -> ObjectUtil.requireAllNonEmpty("不能为空", "a", List.of("b")));
        assertDoesNotThrow(() -> ObjectUtil.requireAnyNonNull("至少一个", null, "a"));
        assertDoesNotThrow(() -> ObjectUtil.requireAnyNonEmpty("至少一个", null, "a"));
        assertThrows(IllegalArgumentException.class, () -> ObjectUtil.requireAllNonNull("不能为空", "a", null));
        assertThrows(IllegalArgumentException.class, () -> ObjectUtil.requireAllNonEmpty("不能为空", "a", ""));
        assertThrows(IllegalArgumentException.class, () -> ObjectUtil.requireAnyNonNull("至少一个", null, null));
        assertThrows(IllegalArgumentException.class, () -> ObjectUtil.requireAnyNonEmpty("至少一个", null, ""));
    }
}
