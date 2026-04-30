package io.github.atengk.system;

import io.github.atengk.utils.SystemUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemUtilPropertyTest {

    @Test
    void shouldSetGetAndClearProperty() {
        String key = "system.util.test.property";
        try {
            SystemUtil.setProperty(key, "123");
            assertEquals("123", SystemUtil.getProperty(key));
            assertTrue(SystemUtil.hasProperty(key));
            assertEquals(123, SystemUtil.getPropertyAsInt(key, 0));
            assertEquals(123L, SystemUtil.getPropertyAsLong(key, 0L));
        } finally {
            SystemUtil.clearProperty(key);
        }
        assertFalse(SystemUtil.hasProperty(key));
    }

    @Test
    void shouldReturnDefaultWhenPropertyMissingOrInvalid() {
        String key = "system.util.invalid.property";
        try {
            assertEquals("default", SystemUtil.getProperty(key, "default"));
            SystemUtil.setProperty(key, "abc");
            assertEquals(7, SystemUtil.getPropertyAsInt(key, 7));
            assertFalse(SystemUtil.getPropertyAsBoolean(key, false));
        } finally {
            SystemUtil.clearProperty(key);
        }
    }

    @Test
    void shouldRejectInvalidPropertyArguments() {
        assertThrows(IllegalArgumentException.class, () -> SystemUtil.getProperty(" "));
        assertThrows(NullPointerException.class, () -> SystemUtil.setProperty("system.util.null", null));
        assertThrows(IllegalStateException.class, () -> SystemUtil.getRequiredProperty("system.util.not.exists"));
        assertNotNull(SystemUtil.getPropertyMap());
    }
}
