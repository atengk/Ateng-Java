package io.github.atengk.datetime;

import io.github.atengk.utils.DateTimeUtil;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateTimeUtilTestClockTest {

    @Test
    void shouldCreateFixedClock() {
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
        LocalDateTime dateTime = LocalDateTime.of(2026, 4, 29, 10, 30, 15);
        Clock clock = DateTimeUtil.fixedClock(dateTime, zoneId);

        assertEquals(dateTime, DateTimeUtil.nowDateTime(clock));
        assertEquals(zoneId, clock.getZone());
    }

    @Test
    void shouldCreateOffsetClock() {
        Clock offsetClock = DateTimeUtil.offsetClock(Duration.ofHours(1));
        long diffSeconds = DateTimeUtil.nowEpochSecond(offsetClock) - DateTimeUtil.nowEpochSecond();

        assertTrue(diffSeconds >= 3_590 && diffSeconds <= 3_610);
    }

    @Test
    void shouldParseTestDateTime() {
        assertEquals(LocalDateTime.of(2026, 4, 29, 10, 30, 15), DateTimeUtil.testDateTime("2026-04-29 10:30:15"));
    }
}
