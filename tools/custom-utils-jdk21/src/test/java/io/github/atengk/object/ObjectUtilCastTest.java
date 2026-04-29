package io.github.atengk.object;

import io.github.atengk.utils.ObjectUtil;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilCastTest {

    @Test
    void shouldCastValues() {
        String value = ObjectUtil.cast("a");
        assertEquals("a", value);
        assertEquals("a", ObjectUtil.cast("a", String.class));
        assertNull(ObjectUtil.castOrNull(1, String.class));
        assertEquals("b", ObjectUtil.castOrDefault(1, String.class, "b"));
        assertEquals("a", ObjectUtil.as("a", String.class));
    }

    @Test
    void shouldReturnOptionalWhenSafeCast() {
        Optional<String> value = ObjectUtil.safeCast("a", String.class);
        assertTrue(value.isPresent());
        assertTrue(ObjectUtil.safeCast(1, String.class).isEmpty());
    }

    @Test
    void shouldThrowWhenCastInvalid() {
        assertThrows(ClassCastException.class, () -> ObjectUtil.cast(1, String.class));
        assertThrows(NullPointerException.class, () -> ObjectUtil.cast("a", null));
        assertThrows(IllegalArgumentException.class, () -> ObjectUtil.castOrThrow(1, String.class, "类型错误"));
        assertThrows(NullPointerException.class, () -> ObjectUtil.castOrThrow("a", null, "类型错误"));
    }
}
