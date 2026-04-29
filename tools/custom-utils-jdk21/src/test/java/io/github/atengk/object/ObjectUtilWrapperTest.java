package io.github.atengk.object;

import io.github.atengk.utils.ObjectUtil;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilWrapperTest {

    @Test
    void shouldGetFromSupplier() {
        assertEquals("a", ObjectUtil.get(() -> "a"));
        assertNull(ObjectUtil.getOrNull(null));
        assertNull(ObjectUtil.getOrNull(() -> { throw new IllegalStateException(); }));
        assertEquals("b", ObjectUtil.getOrDefault(() -> null, "b"));
        assertThrows(NullPointerException.class, () -> ObjectUtil.get(null));
    }

    @Test
    void shouldUnwrapValues() {
        assertEquals("a", ObjectUtil.unwrap(Optional.of("a")));
        assertNull(ObjectUtil.unwrap(Optional.empty()));
        assertEquals("a", ObjectUtil.unwrap((java.util.function.Supplier<String>) () -> "a"));
        assertEquals("x", ObjectUtil.unwrap("x"));
        assertEquals("a", ObjectUtil.unwrapOptional(Optional.of("a")));
        assertEquals("a", ObjectUtil.unwrapSupplier(() -> "a"));
        assertNull(ObjectUtil.unwrapSupplier(null));
    }
}
