package io.github.atengk.object;

import io.github.atengk.utils.ObjectUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilStringTest {

    @Test
    void shouldConvertToString() {
        assertEquals("null", ObjectUtil.toString(null));
        assertEquals("a", ObjectUtil.toString("a"));
        assertEquals("b", ObjectUtil.toString(null, "b"));
        assertEquals("", ObjectUtil.toStringOrEmpty(null));
        assertNull(ObjectUtil.toStringOrNull(null));
        assertEquals("1", ObjectUtil.toStringOrNull(1));
    }

    @Test
    void shouldReturnIdentityStringAndClassName() {
        Object value = new Object();
        assertTrue(ObjectUtil.identityToString(value).startsWith("java.lang.Object@"));
        assertNull(ObjectUtil.identityToString(null));
        assertEquals("String", ObjectUtil.simpleClassName("a"));
        assertEquals("java.lang.String", ObjectUtil.className("a"));
    }
}
