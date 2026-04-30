package io.github.atengk.os;

import io.github.atengk.utils.OsUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OsUtilPropertyTest {

    @Test
    void shouldSetGetAndClearProperty() {
        String key = "os.util.test.property";
        try {
            OsUtil.setProperty(key, "123");
            assertEquals("123", OsUtil.getProperty(key));
            assertTrue(OsUtil.hasProperty(key));
            assertEquals(123, OsUtil.getPropertyAsInt(key, 0));
            assertEquals(123L, OsUtil.getPropertyAsLong(key, 0L));
        } finally {
            OsUtil.clearProperty(key);
        }
        assertFalse(OsUtil.hasProperty(key));
    }

    @Test
    void shouldReturnDefaultWhenPropertyMissingOrInvalid() {
        String key = "os.util.invalid.property";
        try {
            assertEquals("default", OsUtil.getProperty(key, "default"));
            OsUtil.setProperty(key, "abc");
            assertEquals(7, OsUtil.getPropertyAsInt(key, 7));
            assertFalse(OsUtil.getPropertyAsBoolean(key, false));
        } finally {
            OsUtil.clearProperty(key);
        }
    }

    @Test
    void shouldRejectInvalidPropertyArguments() {
        assertThrows(IllegalArgumentException.class, () -> OsUtil.getProperty(" "));
        assertThrows(NullPointerException.class, () -> OsUtil.setProperty("os.util.null", null));
        assertThrows(IllegalStateException.class, () -> OsUtil.getRequiredProperty("os.util.not.exists"));
        assertNotNull(OsUtil.getPropertyMap());
    }
}
