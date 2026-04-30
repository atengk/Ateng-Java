package io.github.atengk.random;

import io.github.atengk.utils.random.RandomUtil;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class RandomUtilDateTimeTest {

    @Test
    void shouldGenerateDateTimeWithinRange() {
        LocalDate date = RandomUtil.randomLocalDate(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        assertFalse(date.isBefore(LocalDate.of(2024, 1, 1)));
        assertFalse(date.isAfter(LocalDate.of(2024, 1, 31)));

        LocalTime time = RandomUtil.randomLocalTime(LocalTime.of(9, 0), LocalTime.of(18, 0));
        assertFalse(time.isBefore(LocalTime.of(9, 0)));
        assertFalse(time.isAfter(LocalTime.of(18, 0)));

        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 2, 0, 0);
        LocalDateTime value = RandomUtil.randomLocalDateTime(start, end);
        assertFalse(value.isBefore(start));
        assertFalse(value.isAfter(end));
    }

    @Test
    void shouldGenerateInstantDatePastFutureAndBirthday() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-02T00:00:00Z");
        Instant instant = RandomUtil.randomInstant(start, end);
        assertFalse(instant.isBefore(start));
        assertFalse(instant.isAfter(end));

        Date date = RandomUtil.randomDate(Date.from(start), Date.from(end));
        assertFalse(date.toInstant().isBefore(start));
        assertFalse(date.toInstant().isAfter(end));

        assertNotNull(RandomUtil.randomPastDate(1));
        assertNotNull(RandomUtil.randomFutureDate(1));
        assertNotNull(RandomUtil.randomBirthday(18, 20));
    }

    @Test
    void shouldHandleDateTimeBoundaries() {
        LocalDate date = LocalDate.of(2024, 1, 1);
        assertEquals(date, RandomUtil.randomLocalDate(date, date));
        LocalTime time = LocalTime.of(10, 0);
        assertEquals(time, RandomUtil.randomLocalTime(time, time));
    }

    @Test
    void shouldRejectInvalidDateTimeArguments() {
        assertThrows(NullPointerException.class, () -> RandomUtil.randomLocalDate(null, LocalDate.now()));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomLocalDate(LocalDate.now(), LocalDate.now().minusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomLocalTime(LocalTime.NOON, LocalTime.MIDNIGHT));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomPastDate(-1));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomBirthday(30, 20));
    }
}
