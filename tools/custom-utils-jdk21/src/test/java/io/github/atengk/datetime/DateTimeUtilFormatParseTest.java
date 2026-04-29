package io.github.atengk.datetime;

import io.github.atengk.utils.DateTimeUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateTimeUtilFormatParseTest {

    @Test
    void shouldFormatDateTimeDateAndTime() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 4, 29, 10, 30, 15);

        assertEquals("2026-04-29 10:30:15", DateTimeUtil.format(dateTime, DateTimeUtil.NORM_DATETIME_PATTERN));
        assertEquals("2026-04-29", DateTimeUtil.format(dateTime.toLocalDate(), DateTimeUtil.NORM_DATE_PATTERN));
        assertEquals("10:30:15", DateTimeUtil.format(dateTime.toLocalTime(), DateTimeUtil.NORM_TIME_PATTERN));
    }

    @Test
    void shouldParseCommonPatterns() {
        assertEquals(LocalDateTime.of(2026, 4, 29, 10, 30, 15), DateTimeUtil.parseDateTime("2026-04-29 10:30:15", DateTimeUtil.NORM_DATETIME_PATTERN));
        assertEquals(LocalDate.of(2026, 4, 29), DateTimeUtil.parseDate("2026-04-29", DateTimeUtil.NORM_DATE_PATTERN));
        assertEquals(LocalTime.of(10, 30, 15), DateTimeUtil.parseTime("10:30:15", DateTimeUtil.NORM_TIME_PATTERN));
    }

    @Test
    void shouldParseSmartPatterns() {
        assertEquals(LocalDateTime.of(2026, 4, 29, 10, 30, 15), DateTimeUtil.parseDateTimeSmart("20260429103015"));
        assertEquals(LocalDateTime.of(2026, 4, 29, 10, 30), DateTimeUtil.parseDateTimeSmart("2026-04-29 10:30"));
        assertEquals(LocalDateTime.of(2026, 4, 29, 0, 0), DateTimeUtil.parseDateTimeSmart("2026-04-29"));
    }

    @Test
    void shouldCheckDateFormat() {
        assertTrue(DateTimeUtil.isDateFormat("2026-04-29 10:30:15", DateTimeUtil.NORM_DATETIME_PATTERN));
        assertTrue(DateTimeUtil.isDateFormat("2026-04-29", DateTimeUtil.NORM_DATE_PATTERN));
        assertFalse(DateTimeUtil.isDateFormat("2026/04/29", DateTimeUtil.NORM_DATE_PATTERN));
    }
}
