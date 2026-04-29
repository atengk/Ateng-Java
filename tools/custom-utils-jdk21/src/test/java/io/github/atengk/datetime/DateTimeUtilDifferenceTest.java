package io.github.atengk.datetime;

import io.github.atengk.utils.DateTimeUtil;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateTimeUtilDifferenceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Clock CLOCK = Clock.fixed(LocalDateTime.of(2026, 4, 29, 10, 0).atZone(ZONE).toInstant(), ZONE);

    @Test
    void shouldCalculateTimeDifferences() {
        LocalDateTime start = LocalDateTime.of(2026, 4, 29, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 4, 29, 12, 30, 15);

        assertEquals(9_015, DateTimeUtil.betweenSeconds(start, end));
        assertEquals(150, DateTimeUtil.betweenMinutes(start, end));
        assertEquals(2, DateTimeUtil.betweenHours(start, end));
        assertEquals(Duration.ofSeconds(9_015), DateTimeUtil.durationBetween(start, end));
    }

    @Test
    void shouldCalculateDateDifferences() {
        assertEquals(9, DateTimeUtil.betweenDays(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 10)));
        assertEquals(2, DateTimeUtil.betweenMonths(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1)));
        assertEquals(3, DateTimeUtil.betweenYears(LocalDate.of(2023, 1, 1), LocalDate.of(2026, 1, 1)));
        assertEquals(Period.of(3, 2, 1), DateTimeUtil.periodBetween(LocalDate.of(2023, 2, 28), LocalDate.of(2026, 4, 29)));
    }

    @Test
    void shouldCalculateAgeAndRemainingText() {
        assertEquals(26, DateTimeUtil.age(LocalDate.of(2000, 4, 29), CLOCK));
        assertEquals("1小时30分钟", DateTimeUtil.remainingText(LocalDateTime.of(2026, 4, 29, 11, 30), CLOCK));
    }
}
