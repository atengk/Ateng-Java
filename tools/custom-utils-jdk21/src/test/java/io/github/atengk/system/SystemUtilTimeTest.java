package io.github.atengk.system;

import io.github.atengk.utils.SystemUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SystemUtilTimeTest {

    @Test
    void shouldGetTimeLocaleAndCharset() {
        assertNotNull(SystemUtil.getDefaultTimeZone());
        assertNotNull(SystemUtil.getDefaultZoneId());
        assertNotNull(SystemUtil.getDefaultLocale());
        assertNotNull(SystemUtil.getCountry());
        assertFalse(SystemUtil.getLanguage().isBlank());
        assertEquals(Charset.defaultCharset(), SystemUtil.getCharset());
        assertFalse(SystemUtil.getFileEncoding().isBlank());
    }

    @Test
    void shouldGetCurrentTimeValues() {
        LocalDateTime now = SystemUtil.getNow();
        assertNotNull(now);
        assertTrue(SystemUtil.getCurrentTimeMillis() > 0);
        assertTrue(SystemUtil.getNanoTime() > 0);
    }

    @Test
    void shouldIncreaseNanoTime() {
        long first = SystemUtil.getNanoTime();
        long second = SystemUtil.getNanoTime();
        assertTrue(second >= first);
    }
}
