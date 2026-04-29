package io.github.atengk.datetime;

import io.github.atengk.utils.DateTimeUtil;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateTimeUtilConversionTest {

    private static final ZoneId UTC = ZoneId.of("UTC");

    @Test
    void shouldConvertInstantAndDateTime() {
        Instant instant = Instant.parse("2026-04-29T10:30:15Z");
        LocalDateTime dateTime = LocalDateTime.of(2026, 4, 29, 10, 30, 15);

        assertEquals(dateTime, DateTimeUtil.toLocalDateTime(instant, UTC));
        assertEquals(instant, DateTimeUtil.toInstant(dateTime, UTC));
    }

    @Test
    void shouldConvertLegacyDate() {
        Instant instant = Instant.parse("2026-04-29T10:30:15Z");
        Date date = Date.from(instant);

        assertEquals(date.toInstant(), DateTimeUtil.toDate(DateTimeUtil.toLocalDateTime(date)).toInstant());
    }

    @Test
    void shouldConvertEpochMilli() {
        long epochMilli = Instant.parse("2026-04-29T10:30:15Z").toEpochMilli();
        assertEquals(LocalDateTime.of(2026, 4, 29, 10, 30, 15), DateTimeUtil.toLocalDateTime(Instant.ofEpochMilli(epochMilli), UTC));
    }

    @Test
    void shouldConvertDateBoundaries() {
        LocalDate date = LocalDate.of(2026, 4, 29);

        assertEquals(LocalDateTime.of(2026, 4, 29, 0, 0), DateTimeUtil.toStartDateTime(date));
        assertEquals(LocalDateTime.of(2026, 4, 29, 23, 59, 59, 999_999_999), DateTimeUtil.toEndDateTime(date));
        assertEquals(LocalDateTime.of(2026, 4, 1, 0, 0), DateTimeUtil.toMonthStart(YearMonth.of(2026, 4)));
        assertEquals(LocalDateTime.of(2026, 4, 30, 23, 59, 59, 999_999_999), DateTimeUtil.toMonthEnd(YearMonth.of(2026, 4)));
    }
}
