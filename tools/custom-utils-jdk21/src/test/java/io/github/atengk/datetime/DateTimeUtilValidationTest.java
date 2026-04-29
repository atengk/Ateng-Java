package io.github.atengk.datetime;

import io.github.atengk.utils.DateTimeUtil;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateTimeUtilValidationTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Clock CLOCK = Clock.fixed(LocalDateTime.of(2026, 4, 29, 10, 0).atZone(ZONE).toInstant(), ZONE);

    @Test
    void shouldCheckNullAndRange() {
        assertTrue(DateTimeUtil.isNull(null));
        assertDoesNotThrow(() -> DateTimeUtil.checkRange(LocalDateTime.of(2026, 4, 29, 10, 0), LocalDateTime.of(2026, 4, 29, 11, 0)));
        assertThrows(IllegalArgumentException.class, () -> DateTimeUtil.checkRange(LocalDateTime.of(2026, 4, 29, 11, 0), LocalDateTime.of(2026, 4, 29, 10, 0)));
    }

    @Test
    void shouldCheckTodayRules() {
        assertDoesNotThrow(() -> DateTimeUtil.checkNotBeforeToday(LocalDate.of(2026, 4, 29), CLOCK));
        assertThrows(IllegalArgumentException.class, () -> DateTimeUtil.checkNotBeforeToday(LocalDate.of(2026, 4, 28), CLOCK));
        assertDoesNotThrow(() -> DateTimeUtil.checkNotAfterToday(LocalDate.of(2026, 4, 29), CLOCK));
        assertThrows(IllegalArgumentException.class, () -> DateTimeUtil.checkNotAfterToday(LocalDate.of(2026, 4, 30), CLOCK));
    }

    @Test
    void shouldCheckBirthdayAndRangeLimit() {
        assertTrue(DateTimeUtil.isValidBirthday(LocalDate.of(2000, 1, 1), CLOCK));
        assertFalse(DateTimeUtil.isValidBirthday(LocalDate.of(2026, 4, 30), CLOCK));
        assertTrue(DateTimeUtil.isRangeWithinDays(LocalDateTime.of(2026, 4, 1, 0, 0), LocalDateTime.of(2026, 4, 10, 0, 0), 10));
        assertFalse(DateTimeUtil.isRangeWithinDays(LocalDateTime.of(2026, 4, 1, 0, 0), LocalDateTime.of(2026, 4, 20, 0, 0), 10));
    }
}
