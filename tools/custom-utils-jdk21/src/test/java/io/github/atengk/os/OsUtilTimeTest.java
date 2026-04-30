package io.github.atengk.os;

import io.github.atengk.utils.OsUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OsUtilTimeTest {

    @Test
    void shouldGetTimeLocaleAndCharset() {
        assertNotNull(OsUtil.getDefaultTimeZone());
        assertNotNull(OsUtil.getDefaultZoneId());
        assertNotNull(OsUtil.getDefaultLocale());
        assertNotNull(OsUtil.getCountry());
        assertFalse(OsUtil.getLanguage().isBlank());
        assertEquals(Charset.defaultCharset(), OsUtil.getCharset());
        assertFalse(OsUtil.getFileEncoding().isBlank());
    }

    @Test
    void shouldGetCurrentTimeValues() {
        LocalDateTime now = OsUtil.getNow();
        assertNotNull(now);
        assertTrue(OsUtil.getCurrentTimeMillis() > 0);
        assertTrue(OsUtil.getNanoTime() > 0);
    }

    @Test
    void shouldIncreaseNanoTime() {
        long first = OsUtil.getNanoTime();
        long second = OsUtil.getNanoTime();
        assertTrue(second >= first);
    }
}
