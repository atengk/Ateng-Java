package io.github.atengk.datetime;

import io.github.atengk.utils.DateTimeUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateTimeUtilPeriodDimensionTest {

    @Test
    void shouldGetWeekAndQuarterInfo() {
        assertTrue(DateTimeUtil.weekOfYear(LocalDate.of(2026, 1, 1)) > 0);
        assertEquals(1, DateTimeUtil.isoWeekOfYear(LocalDate.of(2026, 1, 1)));
        assertEquals(2, DateTimeUtil.quarter(LocalDate.of(2026, 4, 29)));
        assertEquals(4, DateTimeUtil.quarterStartMonth(2));
        assertEquals(6, DateTimeUtil.quarterEndMonth(2));
    }

    @Test
    void shouldGetPeriodRanges() {
        assertEquals(new DateTimeUtil.DateRange(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30)), DateTimeUtil.quarterRange(2026, 2));
        assertEquals(new DateTimeUtil.DateRange(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)), DateTimeUtil.monthRange(YearMonth.of(2026, 4)));
        assertEquals(new DateTimeUtil.DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)), DateTimeUtil.yearRange(2026));
    }

    @Test
    void shouldListMonthsAndDates() {
        assertEquals(List.of(YearMonth.of(2026, 1), YearMonth.of(2026, 2), YearMonth.of(2026, 3)),
                DateTimeUtil.monthsBetween(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 4, 1)));
        assertEquals(List.of(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 3)),
                DateTimeUtil.datesBetween(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 4)));
    }
}
