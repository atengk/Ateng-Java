package io.github.atengk.system;

import io.github.atengk.utils.SystemUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemUtilEnvTest {

    @Test
    void shouldReadEnvWithDefaultValue() {
        String value = SystemUtil.getEnv("SYSTEM_UTIL_NOT_EXISTS", "default-value");
        assertEquals("default-value", value);
        assertNotNull(SystemUtil.getEnvMap());
    }

    @Test
    void shouldParseMissingEnvAsDefaultValue() {
        assertEquals(11, SystemUtil.getEnvAsInt("SYSTEM_UTIL_NOT_EXISTS", 11));
        assertEquals(22L, SystemUtil.getEnvAsLong("SYSTEM_UTIL_NOT_EXISTS", 22L));
        assertTrue(SystemUtil.getEnvAsBoolean("SYSTEM_UTIL_NOT_EXISTS", true));
    }

    @Test
    void shouldHandleEnvBoundaryAndRequiredEnv() {
        assertFalse(SystemUtil.hasEnv(" "));
        assertThrows(IllegalArgumentException.class, () -> SystemUtil.getEnv(" "));
        assertThrows(IllegalStateException.class, () -> SystemUtil.getRequiredEnv("SYSTEM_UTIL_NOT_EXISTS"));
    }

    @Test
    void shouldReadActiveProfileFromSystemProperty() {
        String old = System.getProperty("spring.profiles.active");
        try {
            System.setProperty("spring.profiles.active", "prod");
            assertEquals("prod", SystemUtil.getActiveProfile());
            assertTrue(SystemUtil.isProdEnv());
            System.setProperty("spring.profiles.active", "dev");
            assertTrue(SystemUtil.isDevEnv());
            System.setProperty("spring.profiles.active", "test");
            assertTrue(SystemUtil.isTestEnv());
        } finally {
            if (old == null) {
                System.clearProperty("spring.profiles.active");
            } else {
                System.setProperty("spring.profiles.active", old);
            }
        }
    }
}
