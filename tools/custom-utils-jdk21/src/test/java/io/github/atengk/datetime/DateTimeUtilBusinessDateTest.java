package io.github.atengk.datetime;

import io.github.atengk.utils.DateTimeUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateTimeUtilBusinessDateTest {

    @Test
    void shouldCheckWeekendAndWorkday() {
        Predicate<LocalDate> holiday = date -> date.equals(LocalDate.of(2026, 4, 29));

        assertTrue(DateTimeUtil.isWeekend(LocalDate.of(2026, 5, 2)));
        assertTrue(DateTimeUtil.isWorkday(LocalDate.of(2026, 4, 28)));
        assertFalse(DateTimeUtil.isWorkday(LocalDate.of(2026, 4, 29), holiday));
        assertTrue(DateTimeUtil.isHoliday(LocalDate.of(2026, 4, 29), holiday));
    }

    @Test
    void shouldCalculateWorkdays() {
        Predicate<LocalDate> holiday = date -> date.equals(LocalDate.of(2026, 4, 29)) || date.equals(LocalDate.of(2026, 5, 1));

        assertEquals(LocalDate.of(2026, 5, 4), DateTimeUtil.nextWorkday(LocalDate.of(2026, 5, 1)));
        assertEquals(LocalDate.of(2026, 5, 1), DateTimeUtil.previousWorkday(LocalDate.of(2026, 5, 4)));
        assertEquals(LocalDate.of(2026, 5, 4), DateTimeUtil.plusWorkdays(LocalDate.of(2026, 4, 29), 2, holiday));
        assertEquals(3, DateTimeUtil.betweenWorkdays(LocalDate.of(2026, 4, 27), LocalDate.of(2026, 5, 1), holiday));
    }

    @Test
    void shouldListWorkdaysOfMonth() {
        List<LocalDate> workdays = DateTimeUtil.workdaysOfMonth(YearMonth.of(2026, 2));

        assertEquals(LocalDate.of(2026, 2, 2), workdays.getFirst());
        assertEquals(LocalDate.of(2026, 2, 27), workdays.getLast());
        assertEquals(20, workdays.size());
    }
}
