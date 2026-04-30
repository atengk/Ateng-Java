package io.github.atengk.os;

import io.github.atengk.utils.OsUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OsUtilEnvTest {

    @Test
    void shouldReadEnvWithDefaultValue() {
        String value = OsUtil.getEnv("SYSTEM_UTIL_NOT_EXISTS", "default-value");
        assertEquals("default-value", value);
        assertNotNull(OsUtil.getEnvMap());
    }

    @Test
    void shouldParseMissingEnvAsDefaultValue() {
        assertEquals(11, OsUtil.getEnvAsInt("SYSTEM_UTIL_NOT_EXISTS", 11));
        assertEquals(22L, OsUtil.getEnvAsLong("SYSTEM_UTIL_NOT_EXISTS", 22L));
        assertTrue(OsUtil.getEnvAsBoolean("SYSTEM_UTIL_NOT_EXISTS", true));
    }

    @Test
    void shouldHandleEnvBoundaryAndRequiredEnv() {
        assertFalse(OsUtil.hasEnv(" "));
        assertThrows(IllegalArgumentException.class, () -> OsUtil.getEnv(" "));
        assertThrows(IllegalStateException.class, () -> OsUtil.getRequiredEnv("SYSTEM_UTIL_NOT_EXISTS"));
    }

    @Test
    void shouldReadActiveProfileFromSystemProperty() {
        String old = System.getProperty("spring.profiles.active");
        try {
            System.setProperty("spring.profiles.active", "prod");
            assertEquals("prod", OsUtil.getActiveProfile());
            assertTrue(OsUtil.isProdEnv());
            System.setProperty("spring.profiles.active", "dev");
            assertTrue(OsUtil.isDevEnv());
            System.setProperty("spring.profiles.active", "test");
            assertTrue(OsUtil.isTestEnv());
        } finally {
            if (old == null) {
                System.clearProperty("spring.profiles.active");
            } else {
                System.setProperty("spring.profiles.active", old);
            }
        }
    }
}
