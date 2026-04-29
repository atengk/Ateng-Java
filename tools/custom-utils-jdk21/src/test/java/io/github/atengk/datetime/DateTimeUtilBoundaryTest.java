package io.github.atengk.datetime;

import io.github.atengk.utils.DateTimeUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateTimeUtilBoundaryTest {

    @Test
    void shouldGetDayBoundaries() {
        LocalDate date = LocalDate.of(2026, 4, 29);
        LocalDateTime dateTime = LocalDateTime.of(2026, 4, 29, 10, 30);

        assertEquals(LocalDateTime.of(2026, 4, 29, 0, 0), DateTimeUtil.beginOfDay(date));
        assertEquals(LocalDateTime.of(2026, 4, 29, 0, 0), DateTimeUtil.beginOfDay(dateTime));
        assertEquals(LocalDateTime.of(2026, 4, 29, 23, 59, 59, 999_999_999), DateTimeUtil.endOfDay(date));
        assertEquals(LocalDateTime.of(2026, 4, 29, 23, 59, 59, 999_999_999), DateTimeUtil.endOfDay(dateTime));
    }

    @Test
    void shouldGetWeekMonthQuarterYearBoundaries() {
        LocalDate date = LocalDate.of(2026, 4, 29);

        assertEquals(LocalDate.of(2026, 4, 27), DateTimeUtil.beginOfWeek(date));
        assertEquals(LocalDate.of(2026, 5, 3), DateTimeUtil.endOfWeek(date));
        assertEquals(LocalDate.of(2026, 4, 1), DateTimeUtil.beginOfMonth(date));
        assertEquals(LocalDate.of(2026, 4, 30), DateTimeUtil.endOfMonth(date));
        assertEquals(LocalDate.of(2026, 4, 1), DateTimeUtil.beginOfQuarter(date));
        assertEquals(LocalDate.of(2026, 6, 30), DateTimeUtil.endOfQuarter(date));
        assertEquals(LocalDate.of(2026, 1, 1), DateTimeUtil.beginOfYear(date));
        assertEquals(LocalDate.of(2026, 12, 31), DateTimeUtil.endOfYear(date));
    }

    @Test
    void shouldGetMonthAndYearLength() {
        assertEquals(29, DateTimeUtil.lengthOfMonth(LocalDate.of(2024, 2, 1)));
        assertEquals(366, DateTimeUtil.lengthOfYear(LocalDate.of(2024, 1, 1)));
    }
}
