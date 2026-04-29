package io.github.atengk.object;

import io.github.atengk.utils.ObjectUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilMetaTest {

    @Test
    void shouldGetClassMetadata() {
        assertEquals(String.class, ObjectUtil.getClass("a"));
        assertEquals("java.lang.String", ObjectUtil.getClassName("a"));
        assertEquals("String", ObjectUtil.getSimpleClassName("a"));
        assertEquals("java.lang", ObjectUtil.getPackageName("a"));
        assertEquals("java.lang.String", ObjectUtil.getCanonicalName("a"));
        assertEquals("java.lang.String", ObjectUtil.getTypeName("a"));
    }

    @Test
    void shouldHandleNullMetadata() {
        assertNull(ObjectUtil.getClass(null));
        assertNull(ObjectUtil.getClassName(null));
        assertNull(ObjectUtil.getSimpleClassName(null));
        assertNull(ObjectUtil.getPackageName(null));
        assertNull(ObjectUtil.getCanonicalName(null));
        assertNull(ObjectUtil.getTypeName(null));
    }

    @Test
    void shouldCheckSameClass() {
        assertTrue(ObjectUtil.isSameClass("a", "b"));
        assertFalse(ObjectUtil.isSameClass("a", 1));
        assertFalse(ObjectUtil.isSameClass(null, "a"));
    }
}
