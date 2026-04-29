package io.github.atengk.datetime;

import io.github.atengk.utils.DateTimeUtil;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateTimeUtilQueryRangeTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Clock CLOCK = Clock.fixed(LocalDateTime.of(2026, 4, 29, 10, 0).atZone(ZONE).toInstant(), ZONE);

    @Test
    void shouldBuildDayWeekAndMonthRange() {
        assertEquals(new DateTimeUtil.TimeRange(LocalDateTime.of(2026, 4, 29, 0, 0), LocalDateTime.of(2026, 4, 30, 0, 0)),
                DateTimeUtil.dayRange(LocalDate.of(2026, 4, 29)));
        assertEquals(new DateTimeUtil.TimeRange(LocalDateTime.of(2026, 4, 27, 0, 0), LocalDateTime.of(2026, 5, 4, 0, 0)),
                DateTimeUtil.weekRange(LocalDate.of(2026, 4, 29)));
        assertEquals(new DateTimeUtil.TimeRange(LocalDateTime.of(2026, 4, 1, 0, 0), LocalDateTime.of(2026, 5, 1, 0, 0)),
                DateTimeUtil.monthRange(LocalDate.of(2026, 4, 29)));
    }

    @Test
    void shouldBuildLastRange() {
        assertEquals(new DateTimeUtil.TimeRange(LocalDateTime.of(2026, 4, 22, 10, 0), LocalDateTime.of(2026, 4, 29, 10, 0)), DateTimeUtil.lastDaysRange(7, CLOCK));
        assertEquals(new DateTimeUtil.TimeRange(LocalDateTime.of(2026, 4, 29, 4, 0), LocalDateTime.of(2026, 4, 29, 10, 0)), DateTimeUtil.lastHoursRange(6, CLOCK));
    }

    @Test
    void shouldBuildQueryStartAndExclusiveEnd() {
        assertEquals(LocalDateTime.of(2026, 4, 29, 0, 0), DateTimeUtil.queryStart(LocalDate.of(2026, 4, 29)));
        assertEquals(LocalDateTime.of(2026, 4, 30, 0, 0), DateTimeUtil.queryEndExclusive(LocalDate.of(2026, 4, 29)));
    }
}
