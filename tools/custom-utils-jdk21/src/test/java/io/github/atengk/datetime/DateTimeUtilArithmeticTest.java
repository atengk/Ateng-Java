package io.github.atengk.datetime;

import io.github.atengk.utils.DateTimeUtil;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateTimeUtilArithmeticTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Clock CLOCK = Clock.fixed(LocalDateTime.of(2026, 4, 29, 10, 30).atZone(ZONE).toInstant(), ZONE);

    @Test
    void shouldAddAndMinusDateTime() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 4, 29, 10, 30);

        assertEquals(LocalDateTime.of(2026, 5, 1, 10, 30), DateTimeUtil.plusDays(dateTime, 2));
        assertEquals(LocalDateTime.of(2026, 4, 27, 10, 30), DateTimeUtil.minusDays(dateTime, 2));
        assertEquals(LocalDateTime.of(2026, 4, 29, 12, 30), DateTimeUtil.plusHours(dateTime, 2));
        assertEquals(LocalDateTime.of(2026, 4, 29, 10, 45), DateTimeUtil.plusMinutes(dateTime, 15));
    }

    @Test
    void shouldAddDateUnits() {
        assertEquals(LocalDate.of(2026, 5, 29), DateTimeUtil.plusMonths(LocalDate.of(2026, 4, 29), 1));
        assertEquals(LocalDate.of(2027, 4, 29), DateTimeUtil.plusYears(LocalDate.of(2026, 4, 29), 1));
    }

    @Test
    void shouldGetRelativeDates() {
        assertEquals(LocalDate.of(2026, 4, 26), DateTimeUtil.daysAgo(3, CLOCK));
        assertEquals(LocalDate.of(2026, 5, 2), DateTimeUtil.daysLater(3, CLOCK));
    }

    @Test
    void shouldGetNextRoundedTime() {
        assertEquals(LocalDateTime.of(2026, 4, 29, 11, 0), DateTimeUtil.nextHour(LocalDateTime.of(2026, 4, 29, 10, 30)));
        assertEquals(LocalDateTime.of(2026, 4, 30, 0, 0), DateTimeUtil.nextDayBegin(LocalDateTime.of(2026, 4, 29, 10, 30)));
    }
}
