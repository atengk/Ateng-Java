package io.github.atengk.datetime;

import io.github.atengk.utils.DateTimeUtil;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DateTimeUtilCurrentTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 4, 29, 10, 30, 15);
    private static final Clock CLOCK = Clock.fixed(FIXED_TIME.atZone(ZONE).toInstant(), ZONE);

    @Test
    void shouldGetCurrentValuesByClock() {
        assertEquals(LocalDate.of(2026, 4, 29), DateTimeUtil.nowDate(CLOCK));
        assertEquals(LocalTime.of(10, 30, 15), DateTimeUtil.nowTime(CLOCK));
        assertEquals(FIXED_TIME, DateTimeUtil.nowDateTime(CLOCK));
        assertNotNull(DateTimeUtil.nowDateTime(ZONE));
    }

    @Test
    void shouldGetEpochValuesByClock() {
        assertEquals(FIXED_TIME.atZone(ZONE).toInstant().toEpochMilli(), DateTimeUtil.nowEpochMilli(CLOCK));
        assertEquals(FIXED_TIME.atZone(ZONE).toInstant().getEpochSecond(), DateTimeUtil.nowEpochSecond(CLOCK));
    }

    @Test
    void shouldGetSystemCurrentValues() {
        assertNotNull(DateTimeUtil.nowDate());
        assertNotNull(DateTimeUtil.nowTime());
        assertNotNull(DateTimeUtil.nowDateTime());
        assertNotNull(DateTimeUtil.nowEpochMilli());
        assertNotNull(DateTimeUtil.nowEpochSecond());
    }
}
