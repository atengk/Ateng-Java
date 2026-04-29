package io.github.atengk.object;

import io.github.atengk.utils.ObjectUtil;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilOptionalTest {

    @Test
    void shouldCreateOptional() {
        assertTrue(ObjectUtil.optional("a").isPresent());
        assertTrue(ObjectUtil.optional(null).isEmpty());
        assertTrue(ObjectUtil.optionalIfNotEmpty("").isEmpty());
        assertTrue(ObjectUtil.optionalIfNotEmpty("a").isPresent());
        assertTrue(ObjectUtil.optionalIfNotBlank(" ").isEmpty());
        assertTrue(ObjectUtil.optionalIfNotBlank("a").isPresent());
    }

    @Test
    void shouldUnwrapOptional() {
        assertEquals("a", ObjectUtil.unwrap(Optional.of("a")));
        assertNull(ObjectUtil.unwrap(Optional.empty()));
        assertNull(ObjectUtil.unwrap((Optional<String>) null));
        assertEquals("b", ObjectUtil.unwrapOrDefault(Optional.empty(), "b"));
        assertEquals("a", ObjectUtil.unwrapOrGet(Optional.of("a"), () -> "b"));
        assertEquals("b", ObjectUtil.unwrapOrGet(Optional.empty(), () -> "b"));
    }

    @Test
    void shouldThrowWhenSupplierMissing() {
        assertThrows(NullPointerException.class, () -> ObjectUtil.unwrapOrGet(Optional.empty(), null));
    }
}
