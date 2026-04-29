package io.github.atengk.object;

import io.github.atengk.utils.ObjectUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilDefaultValueTest {

    @Test
    void shouldReturnDefaultValue() {
        assertEquals("b", ObjectUtil.defaultIfNull(null, "b"));
        assertEquals("a", ObjectUtil.defaultIfNull("a", "b"));
        assertEquals("b", ObjectUtil.defaultIfEmpty("", "b"));
        assertEquals("a", ObjectUtil.defaultIfEmpty("a", "b"));
        assertEquals("b", ObjectUtil.defaultIfBlank(" ", "b"));
        assertEquals("a", ObjectUtil.defaultIfBlank("a", "b"));
    }

    @Test
    void shouldReturnLazyDefaultValue() {
        assertEquals("b", ObjectUtil.defaultIfNullGet(null, () -> "b"));
        assertEquals("b", ObjectUtil.defaultIfEmptyGet(List.of(), () -> List.of("b")).getFirst());
        assertEquals("b", ObjectUtil.defaultIfBlankGet(" ", () -> "b"));
    }

    @Test
    void shouldFindFirstAvailableValue() {
        assertEquals("a", ObjectUtil.firstNonNull(null, "a", "b"));
        assertEquals("a", ObjectUtil.firstNonEmpty(null, "", "a"));
        assertEquals("a", ObjectUtil.firstNonBlank(null, " ", "a"));
        assertNull(ObjectUtil.firstNonNull((String[]) null));
    }

    @Test
    void shouldThrowWhenSupplierMissing() {
        assertThrows(NullPointerException.class, () -> ObjectUtil.defaultIfNullGet(null, null));
        assertThrows(NullPointerException.class, () -> ObjectUtil.defaultIfEmptyGet("", null));
        assertThrows(NullPointerException.class, () -> ObjectUtil.defaultIfBlankGet(" ", null));
    }
}
